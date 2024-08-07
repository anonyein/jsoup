package org.jsoup.integration.servlets;

import org.jsoup.integration.TestServer;
import org.jsoup.parser.CharacterReaderTest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InterruptedServlet extends BaseServlet {
    public static final String Url;
    public static final String TlsUrl;
    static {
        TestServer.ServletUrls urls = TestServer.map(InterruptedServlet.class);
        Url = urls.url;
        TlsUrl = urls.tlsUrl;
    }
    public static final String Magnitude = "magnitude";
    public static final String Larger = "larger";

    @Override
    protected void doIt(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String magnitude = req.getParameter(Magnitude);
        magnitude  = magnitude == null ? "" : magnitude;
        res.setContentType(TextHtml);
        res.setStatus(HttpServletResponse.SC_OK);

        StringBuilder sb = new StringBuilder();
        sb.append("<title>Something</title>");
        while (sb.length() <= 32 * 1024) {
            sb.append("<div>A suitable amount of data.</div>\n");
        }
        sb.append("<p>Finale.</p>");
        String data = sb.toString();

        int contentLength = magnitude.equals(Larger) ? data.length() * 2 : data.length() / 2;
        res.setContentLength(contentLength);

        res.getWriter().write(data);

    }
}
