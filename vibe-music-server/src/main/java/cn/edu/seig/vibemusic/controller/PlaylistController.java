package cn.edu.seig.vibemusic.controller;


import cn.edu.seig.vibemusic.model.dto.PlaylistAddDTO;
import cn.edu.seig.vibemusic.model.dto.PlaylistDTO;
import cn.edu.seig.vibemusic.model.dto.PlaylistUpdateDTO;
import cn.edu.seig.vibemusic.model.vo.PlaylistDetailVO;
import cn.edu.seig.vibemusic.model.vo.PlaylistVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IPlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

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
@RequestMapping("/playlist")
public class PlaylistController {

    @Autowired
    private IPlaylistService playlistService;

    /**
     * 获取所有歌单
     *
     * @param playlistDTO playlistDTO
     * @return 歌单列表
     */
    @PostMapping("/getAllPlaylists")
    public Result<PageResult<PlaylistVO>> getAllPlaylists(@RequestBody @Valid PlaylistDTO playlistDTO) {
        return playlistService.getAllPlaylists(playlistDTO);
    }

    /**
     * 获取推荐歌单
     *
     * @param request request
     * @return 推荐歌单列表
     */
    @GetMapping("/getRecommendedPlaylists")
    public Result<List<PlaylistVO>> getRandomPlaylists(HttpServletRequest request) {
        return playlistService.getRecommendedPlaylists(request);
    }

    /**
     * 获取歌单详情
     *
     * @param playlistId 歌单id
     * @return 歌单详情
     */
    @GetMapping("/getPlaylistDetail/{id}")
    public Result<PlaylistDetailVO> getPlaylistDetail(@PathVariable("id") Long playlistId, HttpServletRequest request) {
        return playlistService.getPlaylistDetail(playlistId, request);
    }

    /**
     * 新增歌单
     *
     * @param playlistAddDTO 歌单信息
     * @return 结果
     */
    @PostMapping("/addPlaylist")
    public Result addPlaylist(@RequestBody PlaylistAddDTO playlistAddDTO) {
        return playlistService.addPlaylist(playlistAddDTO);
    }

    /**
     * 更新歌单信息
     *
     * @param playlistUpdateDTO 歌单信息
     * @return 结果
     */
    @RequestMapping(value = "/updatePlaylist", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result updatePlaylist(@RequestBody PlaylistUpdateDTO playlistUpdateDTO) {
        return playlistService.updatePlaylist(playlistUpdateDTO);
    }

    /**
     * 删除歌单
     *
     * @param playlistId 歌单id
     * @return 结果
     */
    @RequestMapping(value = "/deletePlaylist/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public Result deletePlaylist(@PathVariable("id") Long playlistId) {
        return playlistService.deletePlaylist(playlistId);
    }

    /**
     * 向歌单添加歌曲
     *
     * @param playlistId 歌单id
     * @param songId     歌曲id
     * @return 添加结果
     */
    @PostMapping("/addSong")
    public Result addSongToPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        return playlistService.addSongToPlaylist(playlistId, songId);
    }

    /**
     * 从歌单移除歌曲
     *
     * @param playlistId 歌单id
     * @param songId     歌曲id
     * @return 移除结果
     */
    @RequestMapping(value = "/removeSong", method = {RequestMethod.DELETE, RequestMethod.POST})
    public Result removeSongFromPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        return playlistService.removeSongFromPlaylist(playlistId, songId);
    }

}
