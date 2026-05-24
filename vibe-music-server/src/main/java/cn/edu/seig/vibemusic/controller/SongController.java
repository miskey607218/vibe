package cn.edu.seig.vibemusic.controller;


import cn.edu.seig.vibemusic.model.dto.SongDTO;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.model.vo.SongDetailVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.ISongService;
import cn.edu.seig.vibemusic.service.MinioService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/song")
public class SongController {

    @Autowired
    private ISongService songService;
    @Autowired
    private MinioService minioService;

    // 用于流式传输的 Mapper 直接查询
    @Autowired
    private cn.edu.seig.vibemusic.mapper.SongMapper songMapper;

    /**
     * 获取所有歌曲
     *
     * @param songDTO songDTO
     * @return 歌曲列表
     */
    @PostMapping("/getAllSongs")
    public Result<PageResult<SongVO>> getAllSongs(@RequestBody @Valid SongDTO songDTO, HttpServletRequest request) {
        return songService.getAllSongs(songDTO, request);
    }

    /**
     * 获取推荐歌曲
     * 推荐歌曲的数量为 20
     *
     * @param request 请求
     * @return 推荐歌曲列表
     */
    @GetMapping("/getRecommendedSongs")
    public Result<List<SongVO>> getRecommendedSongs(HttpServletRequest request) {
        return songService.getRecommendedSongs(request);
    }

    /**
     * 获取歌曲详情
     *
     * @param songId 歌曲id
     * @return 歌曲详情
     */
    @GetMapping("/getSongDetail/{id}")
    public Result<SongDetailVO> getSongDetail(@PathVariable("id") Long songId, HttpServletRequest request) {
        return songService.getSongDetail(songId, request);
    }

    /**
     * 音频流代理（从 MinIO 获取并流式传输，支持 Range 请求）
     */
    @GetMapping("/stream/{id}")
    public void streamAudio(@PathVariable("id") Long songId,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        Logger log = LoggerFactory.getLogger(SongController.class);
        log.info("Stream request: songId={}, Range={}", songId, request.getHeader("Range"));
        try {
            Song song = songMapper.selectById(songId);
            if (song == null || song.getAudioUrl() == null) {
                log.warn("Stream: song not found or no audioUrl for id={}", songId);
                response.setStatus(404);
                return;
            }

            log.info("Stream: fetching from MinIO: {}", song.getAudioUrl());
            InputStream inputStream = minioService.getFileStream(song.getAudioUrl());

            response.setContentType("audio/mpeg");
            response.setHeader("Accept-Ranges", "bytes");

            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                outputStream.flush();
            } finally {
                inputStream.close();
            }
            log.info("Stream: completed songId={}, bytes={}", songId, totalBytes);
        } catch (Exception e) {
            log.error("Stream: error for songId={}: {}", songId, e.getMessage());
            response.setStatus(500);
        }
    }

}
