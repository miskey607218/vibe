<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { message } from "@/utils/message";
import {
  getPlaylistSongs,
  getSongList,
  addSongToPlaylist,
  removeSongFromPlaylist
} from "@/api/system";

const props = defineProps<{
  playlistId: number;
  playlistTitle: string;
}>();

const activeTab = ref("current");
const currentSongs = ref<any[]>([]);
const loadingCurrent = ref(false);

const searchKeyword = ref("");
const availableSongs = ref<any[]>([]);
const selectedSongIds = ref<Set<number>>(new Set());
const loadingSearch = ref(false);
const searchPageNum = ref(1);
const searchPageSize = ref(30);
const searchTotal = ref(0);

const currentSongIdSet = computed(() => new Set(currentSongs.value.map(s => s.songId)));

const fetchCurrentSongs = async () => {
  loadingCurrent.value = true;
  try {
    const res = await getPlaylistSongs(props.playlistId);
    if (res.code === 0 && res.data) {
      const data = res.data as any;
      currentSongs.value = (data.songs || []).map((s: any) => ({
        songId: s.songId,
        songName: s.songName,
        artistName: s.artistName,
        album: s.album,
        duration: s.duration,
        coverUrl: s.coverUrl
      }));
    }
  } catch {
    message("获取歌单歌曲失败", { type: "error" });
  } finally {
    loadingCurrent.value = false;
  }
};

const searchSongs = async () => {
  loadingSearch.value = true;
  try {
    const res = await getSongList({
      pageNum: searchPageNum.value,
      pageSize: searchPageSize.value,
      songName: searchKeyword.value || undefined
    });
    if (res.code === 0 && res.data) {
      const data = res.data as any;
      availableSongs.value = (data.items || data.records || []).map((item: any) => ({
        songId: item.songId,
        songName: item.songName || item.name,
        artistName: item.artistName,
        album: item.album,
        duration: item.duration,
        coverUrl: item.coverUrl || item.cover_url
      }));
      searchTotal.value = data.total || 0;
    }
  } catch {
    message("搜索歌曲失败", { type: "error" });
  } finally {
    loadingSearch.value = false;
  }
};

const toggleSelect = (songId: number) => {
  const next = new Set(selectedSongIds.value);
  if (next.has(songId)) {
    next.delete(songId);
  } else {
    next.add(songId);
  }
  selectedSongIds.value = next;
};

const isSelected = (songId: number) => selectedSongIds.value.has(songId);

const handleAddSelected = async () => {
  if (selectedSongIds.value.size === 0) {
    ElMessage.warning("请选择要添加的歌曲");
    return;
  }
  let success = 0;
  let fail = 0;
  for (const songId of selectedSongIds.value) {
    try {
      const res = await addSongToPlaylist(props.playlistId, songId);
      if (res.code === 0) {
        success++;
      } else {
        fail++;
      }
    } catch {
      fail++;
    }
  }
  if (success > 0) {
    ElMessage.success(`成功添加 ${success} 首歌曲${fail > 0 ? `，${fail} 首失败` : ""}`);
    selectedSongIds.value = new Set();
    await fetchCurrentSongs();
  } else {
    ElMessage.error("添加失败");
  }
};

const handleRemove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要从歌单中移除「${row.songName}」吗？`, "确认移除", {
      type: "warning"
    });
    const res = await removeSongFromPlaylist(props.playlistId, row.songId);
    if (res.code === 0) {
      ElMessage.success("已移除");
      await fetchCurrentSongs();
    } else {
      ElMessage.error(res.message || "移除失败");
    }
  } catch {
    // cancelled
  }
};

const loadMore = () => {
  searchPageNum.value++;
  searchSongs();
};

onMounted(() => {
  fetchCurrentSongs();
});
</script>

<template>
  <div class="flex flex-col gap-4">
    <div class="flex items-center gap-2 text-sm text-gray-500">
      <span>歌单：</span>
      <span class="font-medium text-gray-800">{{ props.playlistTitle }}</span>
      <span class="ml-2">({{ currentSongs.length }} 首)</span>
    </div>

    <div class="border-b">
      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button value="current">当前歌曲</el-radio-button>
        <el-radio-button value="add">添加歌曲</el-radio-button>
      </el-radio-group>
    </div>

    <div v-show="activeTab === 'current'" class="max-h-[400px] overflow-y-auto">
      <div v-if="loadingCurrent" class="text-center py-8 text-gray-400">加载中...</div>
      <div v-else-if="currentSongs.length === 0" class="text-center py-8 text-gray-400">
        该歌单暂无歌曲
      </div>
      <div v-else v-for="song in currentSongs" :key="song.songId"
        class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition-colors">
        <div class="w-8 h-8 rounded bg-gray-200 overflow-hidden shrink-0">
          <img v-if="song.coverUrl" :src="song.coverUrl" class="w-full h-full object-cover" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-sm truncate">{{ song.songName }}</div>
          <div class="text-xs text-gray-400 truncate">{{ song.artistName }}</div>
        </div>
        <div class="text-xs text-gray-400 hidden sm:block w-24 truncate">{{ song.album }}</div>
        <el-button type="danger" size="small" text @click="handleRemove(song)">移除</el-button>
      </div>
    </div>

    <div v-show="activeTab === 'add'" class="flex flex-col gap-3">
      <div class="flex gap-2">
        <el-input v-model="searchKeyword" placeholder="搜索歌曲名称..." clearable class="flex-1"
          @keyup.enter="searchPageNum = 1; searchSongs()" @clear="searchPageNum = 1; searchSongs()" />
        <el-button type="primary" @click="searchPageNum = 1; searchSongs()">搜索</el-button>
      </div>

      <div class="max-h-[320px] overflow-y-auto">
        <div v-if="loadingSearch" class="text-center py-8 text-gray-400">搜索中...</div>
        <div v-else-if="availableSongs.length === 0" class="text-center py-8 text-gray-400">
          请输入关键词搜索歌曲
        </div>
        <div v-else v-for="song in availableSongs" :key="song.songId"
          class="flex items-center gap-3 p-2 rounded-lg cursor-pointer transition-colors"
          :class="currentSongIdSet.has(song.songId) ? 'opacity-50 bg-gray-50' : 'hover:bg-gray-50'"
          @click="currentSongIdSet.has(song.songId) ? null : toggleSelect(song.songId)">
          <el-checkbox :model-value="isSelected(song.songId)" :disabled="currentSongIdSet.has(song.songId)"
            @click.stop />
          <div class="w-8 h-8 rounded bg-gray-200 overflow-hidden shrink-0">
            <img v-if="song.coverUrl" :src="song.coverUrl" class="w-full h-full object-cover" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm truncate">{{ song.songName }}</div>
            <div class="text-xs text-gray-400 truncate">{{ song.artistName }}</div>
          </div>
          <div class="text-xs text-gray-400 shrink-0">
            {{ currentSongIdSet.has(song.songId) ? '已在歌单中' : song.album }}
          </div>
        </div>
      </div>

      <div v-if="searchTotal > searchPageNum * searchPageSize" class="text-center">
        <el-button text @click="loadMore">加载更多</el-button>
      </div>

      <div class="flex justify-end">
        <el-button type="primary" :disabled="selectedSongIds.size === 0" @click="handleAddSelected">
          添加选中歌曲 ({{ selectedSongIds.size }})
        </el-button>
      </div>
    </div>
  </div>
</template>
