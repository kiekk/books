package com.example.javajigi.webserver;

import com.example.javajigi.db.DataBase;
import com.example.javajigi.http.HttpRequest;
import com.example.javajigi.http.HttpResponse;
import com.example.javajigi.model.User;
import com.example.javajigi.util.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            HttpRequest request = new HttpRequest(in);
            HttpResponse response = new HttpResponse(out);
            String path = getDefaultPath(request.getPath());

            if ("/user/create".equals(path)) {
                User user = new User(
                        request.getParameter("userId"),
                        request.getParameter("password"),
                        request.getParameter("name"),
                        request.getParameter("email")
                );
                log.debug("User : {}", user);

                DataBase.addUser(user);
                response.sendRedirect("/index./html");
            } else if (path.endsWith(".css")) {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
                response200CssHeader(dos, body.length);
                response.forwardBody(path);
            } else if ("/user/login".equals(path)) {
                User user = DataBase.findUserById(request.getParameter("userId"));
                if (user != null && user.login(request.getParameter("password"))) {
                    response.addHeader("Set-Cookie", "logined=true");
                } else {
                    response.sendRedirect("/user/login_failed.html");
                }
            } else if ("/user/list".equals(path)) {
                if (!isLogin(request.getHeader("Cookie"))) {
                    response.sendRedirect("/user/login.html");
                    return;
                }

                Collection<User> users = DataBase.findAll();
                StringBuilder sb = new StringBuilder();

                sb.append("<table border='1'>");
                for (User user : users) {
                    sb.append("<tr>");
                    sb.append("<td>" + user.getUserId() + "</td>");
                    sb.append("<td>" + user.getName() + "</td>");
                    sb.append("<td>" + user.getEmail() + "</td>");
                    sb.append("</tr>");
                }

                sb.append("</table>");
                response.forwardBody(sb.toString());
            } else {
                response.forward(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isLogin(String line) {
        String[] headerTokens = line.split(":");
        Map<String, String> cookies = HttpRequestUtils.parseCookies(headerTokens[1].trim());
        String value = cookies.get("logined");

        if (value == null) {
            return false;
        }

        return Boolean.parseBoolean(value);
    }

    private String getDefaultPath(String path) {
        return path.equals("/") ? "/index.html" : path;
    }
}