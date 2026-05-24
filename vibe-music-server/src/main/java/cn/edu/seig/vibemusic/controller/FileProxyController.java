package cn.edu.seig.vibemusic.controller;

import cn.edu.seig.vibemusic.service.MinioService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;

@RestController
public class FileProxyController {

    @Autowired
    private MinioService minioService;

    @GetMapping("/file/**")
    public void proxyFile(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI().replaceFirst("^/file/", "");
        try {
            String baseUrl = "http://localhost:9000/vibe-music-data/";
            InputStream is = minioService.getFileStream(baseUrl + path);
            String ct = path.endsWith(".png") ? "image/png" : path.endsWith(".jpg") || path.endsWith(".jpeg") ? "image/jpeg" : "application/octet-stream";
            response.setContentType(ct);
            OutputStream os = response.getOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
            os.flush(); is.close();
        } catch (Exception e) { response.setStatus(404); }
    }
}
