package com.oracle.anc.fmwpoc.sf.view.beans;


import com.oracle.anc.fmwpoc.sf.view.utils.ADFUtils;
import com.oracle.anc.fmwpoc.sf.view.utils.JSFUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import oracle.adf.model.BindingContext;
import oracle.adf.view.faces.bi.component.gauge.UIGauge;
import oracle.adf.view.faces.bi.component.graph.UIGraph;
import oracle.adf.view.faces.bi.component.imageView.ShapeAttributesCallback;
import oracle.adf.view.faces.bi.component.imageView.ShapeAttributesSet;
import oracle.adf.view.faces.bi.component.imageView.UIImageView;
import oracle.adf.view.faces.bi.model.DataModel;

import oracle.adfinternal.view.faces.bi.renderkit.imageView.ImageViewRendererUtils;
import oracle.adfinternal.view.faces.bi.renderkit.imageView.ImageViewSVGWriterProviderImpl;
import oracle.adfinternal.view.faces.bi.renderkit.imageView.UIImageViewWrapper;
import oracle.adfinternal.view.faces.bi.util.FileUtils;
import oracle.adfinternal.view.faces.bi.util.JsfUtils;
import oracle.adfinternal.view.faces.dvt.model.binding.gauge.FacesGaugeBinding;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.dss.dataView.ImageView;
import oracle.dss.dataView.SVGWriterProvider;

import org.apache.myfaces.trinidad.render.CoreRenderer;


public class FunnelBean {
    private UIGauge targetGauge;
    private UIGraph funnelGraph;

    public FunnelBean() {
    }

    public void setTargetGauge(UIGauge targetGauge) {
        this.targetGauge = targetGauge;
    }

    public UIGauge getTargetGauge() {
        return targetGauge;
    }

    public synchronized String getTargetGaugeImage() {
        oracle.jbo.domain.Number empId =
            (oracle.jbo.domain.Number)JSFUtils.resolveExpression("#{node.EmpId}");
        setOperationParam("ExecuteTargetWithParams", "pQuarter",
                          getQuarter(false));
        executeWithParam("ExecuteTargetWithParams", "pEmpId", empId);
        BindingContext bcx = (BindingContext)BindingContext.getCurrent();
        DataModel dm =
            ((FacesGaugeBinding)bcx.getCurrentBindingsEntry().getControlBinding("EmpCurrentQuarterReportView")).getDataModel();
        UIGauge gauge = getTargetGauge();
        gauge.setRendered(true);
        gauge.setDataModel(dm);
        gauge.transferProperties();
        String url = "";
        try {
            url =
saveAndRenderImage(false, FacesContext.getCurrentInstance(), gauge.getImageView(),
                   gauge, null, "PNG", "GaugeServlet");
        } catch (IOException e) {
        }
        gauge.setRendered(false);
        gauge = null;
        url = url.substring(url.indexOf("/", 1));
        return url;
    }

    private String getQuarter(boolean next) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (next)
            cal.add(Calendar.MONTH, 3);
        int month = cal.get(Calendar.MONTH);
        return ((Integer)cal.get(Calendar.YEAR)).toString() + "\\" +
            ((Integer)((month / 3) + 1)).toString();
    }

    //This method was provided in previous version of ADF, but has been dropped

    public static String saveAndRenderImage(boolean isFull,
                                            FacesContext context,
                                            ImageView cmnView,
                                            UIComponent component,
                                            ResponseWriter writer,
                                            String imageFormat,
                                            String servletName) throws IOException {
        boolean isSaved = false;
        boolean isPreview = false;
        String urlStr = null;
        boolean isSVG = false;
        if (imageFormat.equals("SVG")) {
            isSVG = true;
        }

        isPreview = false; //isJDevDesignView(context);

        Object session = context.getExternalContext().getSession(true);
        int svgMode = -1;
        if (isSVG) {
            UIImageView uiImgView = (UIImageView)component;
            svgMode =
                    ImageViewRendererUtils.getViewSVGMode(svgMode, cmnView, uiImgView);
            String parentFormName = uiImgView.getSvgParentFormClientId();
            if (parentFormName == null) {
                parentFormName =
                        JsfUtils.getParentFormClientId(uiImgView, context);
            }
            SVGWriterProvider provider = cmnView.getSVGWriterProvider();
            ShapeAttributesSet shapeAttrsSet =
                (ShapeAttributesSet)uiImgView.getFacesBean().getProperty(UIImageView.SHAPE_ATTRIBUTES_SET_KEY);


            ShapeAttributesCallback shapeAttrsCallback =
                uiImgView.getShapeAttributesCallback();
            if ((provider != null) &&
                ((provider instanceof ImageViewSVGWriterProviderImpl))) {
                ((ImageViewSVGWriterProviderImpl)provider).init(context,
                                                                parentFormName,
                                                                shapeAttrsSet,
                                                                shapeAttrsCallback,
                                                                uiImgView.isPartialSubmit());
            }
        }
        String id = ImageViewRendererUtils.constructID(component);
        Writer svgWriter = null;
        OutputStream svgOutputStream = null;
        if ((component instanceof UIImageView)) {
            UIImageView uiImgView = (UIImageView)component;
            svgWriter = uiImgView.getSvgWriter();
            svgOutputStream = uiImgView.getSvgOutputStream();
        }

        if ((isSVG) && (UIImageViewWrapper.getDisposition(component) == 1) &&
            ((svgWriter != null) || (svgOutputStream != null))) {
            try {
                if (svgWriter != null)
                    cmnView.exportToSVGWithException(svgWriter, svgMode, null);
                else if (svgOutputStream != null)
                    cmnView.exportToSVGWithException(svgOutputStream, svgMode,
                                                     null);
            } catch (IOException e1) {
                throw e1;
            } catch (Exception e2) {
                //_LOG.severe("Could not export image to given stream or writer", e2);
            }
        } else if (isPreview) {
            String server_dir =
                FileUtils.getImageServerPath(context, component);
            File dir = new File(server_dir);
            boolean is_dir = dir.isDirectory();
            if ((!is_dir) && (!dir.exists()))
                is_dir = dir.mkdirs();
            if (is_dir) {
                String prefix =
                    new StringBuilder().append("g").append(String.valueOf(System.currentTimeMillis())).toString();
                String suffix = ".png";
                if (isSVG)
                    suffix = ".svg";
                File image = File.createTempFile(prefix, suffix, dir);
                if (image != null) {
                    BufferedOutputStream out_stream =
                        new BufferedOutputStream(new FileOutputStream(image));
                    try {
                        if (isSVG)
                            cmnView.exportToSVGWithException(out_stream,
                                                             svgMode, null);
                        else
                            cmnView.exportToPNGWithException(out_stream);
                        out_stream.close();
                        FileUtils.registerForDeletion(context, component,
                                                      image, id, isPreview);
                        StringBuilder url = new StringBuilder(50);
                        String requestContextPath =
                            context.getExternalContext().getRequestContextPath();
                        url.append(requestContextPath);
                        url.append("/bi/");
                        url.append(image.getName());
                        if (isSVG)
                            writer.writeAttribute("src", url.toString(), null);
                        else
                            urlStr = url.toString();
                        isSaved = true;
                    } catch (IOException e1) {
                        throw e1;
                    } catch (Exception e2) {
                        // _LOG.severe("Could not export image to be saved on disk", e2);
                    }
                }
            }

            if (!isSaved) {
                // _LOG.warning("saving Graph image to disk failed");
            }
        } else if (session != null) {
            ByteArrayOutputStream stream =
                new SerializableByteArrayStream(10240);
            try {
                if (isSVG)
                    cmnView.exportToSVGWithException(stream, svgMode, null);
                else
                    cmnView.exportToPNGWithException(stream);
                stream.close();
                String random = component.getClientId(context);
                random =
                        new StringBuilder().append(random).append((int)(100000000.0D *
                                                                        Math.random())).toString();
                Map sessionMap = context.getExternalContext().getSessionMap();
                if (!isSVG)
                    sessionMap.put(new StringBuilder().append(id).append(random).toString(),
                                   stream);
                else {
                    sessionMap.put(id, stream);
                }
                StringBuilder dest = new StringBuilder(100);

                dest.append(new StringBuilder().append("/servlet/").append(servletName).append("?").toString());
                dest.append("id");
                dest.append('=');
                dest.append(id);

                //dest.append("&random=");
                dest.append(random);
                String url =
                    CoreRenderer.toResourceUri(context, dest.toString());
                dest =
new StringBuilder(context.getExternalContext().encodeResourceURL(url));
                if (isSVG) {
                    writer.writeAttribute("src", dest.toString(), null);
                } else
                    urlStr = dest.toString();
                isSaved = true;
            } catch (IOException e1) {
                throw e1;
            } catch (Exception e2) {
                // _LOG.severe("Could not export image to be saved on session", e2);
            }
        } else {
            // _LOG.warning("saving Graph image to session failed");
        }
        return urlStr;
    }


    private void executeWithParam(String operationBinding, String paramName,
                                  Object param) {
        OperationBinding op =
            setOperationParam(operationBinding, paramName, param);
        op.execute();
    }

    private OperationBinding setOperationParam(String operationBinding,
                                               String paramName,
                                               Object param) {
        BindingContainer dc = ADFUtils.getDCBindingContainer();
        OperationBinding op = dc.getOperationBinding(operationBinding);
        op.getParamsMap().put(paramName, param);
        return op;
    }

    public void setFunnelGraph(UIGraph funnelGraph) {
        this.funnelGraph = funnelGraph;
    }

    public UIGraph getFunnelGraph() {
        return funnelGraph;
    }

    //helper class for saveAndRenderImage

    public static class SerializableByteArrayStream extends ByteArrayOutputStream implements Serializable {
        SerializableByteArrayStream(int size) {
            super();
        }

        private void writeObject(ObjectOutputStream o) throws IOException {
            o.writeInt(size());
            writeTo(o);
        }

        private void readObject(ObjectInputStream i) throws IOException,
                                                            ClassNotFoundException {
            int size = i.readInt();
            byte[] data = new byte[size];
            i.readFully(data);
            write(data);
        }
    }

    public synchronized String getFunnelChartImage() {
        oracle.jbo.domain.Number empId =
            (oracle.jbo.domain.Number)JSFUtils.resolveExpression("#{node.EmpId}");
        executeWithParam("ExecuteFunnelWithParams", "pEmpId", empId);
        getFunnelGraph().transferProperties();
        String url = "";
        try {
            url =
saveAndRenderImage(false, FacesContext.getCurrentInstance(), getFunnelGraph().getImageView(),
                   getFunnelGraph(), null, "PNG", "GraphServlet");
        } catch (IOException e) {
        }
        url = url.substring(url.indexOf("/", 1));
        return url;
    }
}
