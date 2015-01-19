package com.oracle.anc.fmwpoc.sf.view.servlets;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialBlob;

import oracle.jbo.domain.BlobDomain;

public class ImageServlet extends HttpServlet {
    private static final String CONTENT_TYPE = "image/jpg;"; //charset=utf-8
    private Connection cn = null;
    private PreparedStatement statementEmp = null;

    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);
            cn = getConnection();
            statementEmp =
                    cn.prepareStatement("select emp_photo from employees where emp_id=?",
                                        0);
        } catch (SQLException e) {
        }
    }

    public void destroy() {
        try {
            if (statementEmp != null)
                statementEmp.close();
            if (cn != null)
                cn.close();
        } catch (SQLException e) {
        }

    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException,
                                                           IOException {
        response.setContentType(CONTENT_TYPE);
        response.addHeader("Cache-Control", "max-age=0");
        String empId = request.getParameter("empId");
        String oppId = request.getParameter("oppId");
        OutputStream os = response.getOutputStream();
        try {
            if (empId != null) {
                processEmp(empId, os, request);
            }
            response.flushBuffer();
        } catch (Exception e) {
            System.out.println(e);
        } finally {
        }
    }

    private void processEmp(String empId, OutputStream os,
                            HttpServletRequest request) throws SQLException,
                                                               IOException {
        Blob blob = null;
        //this part is serving image during HTML5 upload
        if (request.getSession().getAttribute("tmpPhoto") != null) {
            blob =
new SerialBlob(((BlobDomain)request.getSession().getAttribute("tmpPhoto")).getStorageByteArray());
            request.getSession().removeAttribute("tmpPhoto");
        }
        if (blob == null) {
            synchronized (this) {
                statementEmp.setInt(1, Integer.parseInt(empId));
                ResultSet rs = statementEmp.executeQuery();
                if (rs.next())
                    blob = rs.getBlob("EMP_PHOTO");
            }
        }
        if (blob != null) {
            BufferedInputStream in =
                new BufferedInputStream(blob.getBinaryStream());
            int b;
            byte[] buffer = new byte[10240];
            while ((b = in.read(buffer, 0, 10240)) != -1) {
                os.write(buffer, 0, b);
            }
        }
    }


    private Connection getConnection() {
        Connection connection = null;
        try {
            InitialContext context = new InitialContext();
            DataSource dataSource = (DataSource)context.lookup("jdbc/sfDS");
            connection = dataSource.getConnection();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
