<script setup lang="ts">
import { ref, watch } from "vue";
import ReCol from "@/components/ReCol";
import { formRules } from "../utils/rule";
import { FormProps } from "../utils/types";
import { getAllArtists } from "@/api/system";

const props = withDefaults(defineProps<FormProps>(), {
  formInline: () => ({
    title: "新增",
    artistId: null,
    artistName: "",
    artistList: [],
    songId: null,
    songName: "",
    album: "",
    style: [],
    releaseTime: null,
    duration: "",
    audioFile: null
  })
});

const styleOptions = [
  "节奏布鲁斯", "欧美流行", "华语流行", "粤语流行", "国风流行",
  "韩国流行", "日本流行", "嘻哈说唱", "非洲节拍", "原声带",
  "轻音乐", "摇滚", "朋克", "电子", "国风", "乡村", "古典"
];

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);
const artistOptions = ref<Array<{ label: string; value: number }>>([]);
const audioFileName = ref("");
const audioDuration = ref("");

watch(() => props.formInline, (val) => {
  newFormInline.value = val;
}, { immediate: true, deep: true });

async function fetchArtists() {
  try {
    const res = await getAllArtists();
    if (res.code === 0 && Array.isArray(res.data)) {
      artistOptions.value = res.data.map((item: any) => ({
        label: item.artistName || item.name,
        value: item.artistId || item.id
      }));
    }
  } catch (e) { console.error(e); }
}
if (props.formInline.title === "新增") {
  fetchArtists();
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function handleAudioChange(event: Event) {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    newFormInline.value.audioFile = input.files[0] as any;
    audioFileName.value = input.files[0].name;
    const audio = new Audio();
    audio.src = URL.createObjectURL(input.files[0]);
    audio.addEventListener('loadedmetadata', () => {
      newFormInline.value.duration = String(Math.round(audio.duration));
      audioDuration.value = formatDuration(audio.duration);
      URL.revokeObjectURL(audio.src);
    });
  }
}

function getRef() {
  return ruleFormRef.value;
}
defineExpose({ getRef });
</script>

<template>
  <el-form ref="ruleFormRef" :model="newFormInline" :rules="formRules" label-width="82px">
    <el-row :gutter="30">
      <!-- 新增模式：歌手选择器 -->
      <re-col v-if="newFormInline.title === '新增'" :value="12" :xs="24" :sm="24">
        <el-form-item label="歌手" prop="artistId" required>
          <el-select v-model="newFormInline.artistId" placeholder="请选择歌手" filterable clearable class="w-full">
            <el-option v-for="item in artistOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </re-col>

      <!-- 修改模式：歌手名称只读 -->
      <re-col v-if="newFormInline.title === '修改'" :value="12" :xs="24" :sm="24">
        <el-form-item label="歌手">
          <el-input v-model="newFormInline.artistName" disabled placeholder="" />
        </el-form-item>
      </re-col>

      <!-- 修改模式：歌曲编号 -->
      <re-col v-if="newFormInline.title === '修改'" :value="12" :xs="24" :sm="24">
        <el-form-item label="歌曲编号">
          <el-input :model-value="newFormInline.songId" disabled />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="歌名" prop="songName" required>
          <el-input v-model="newFormInline.songName" clearable placeholder="请输入歌名" />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="专辑" prop="album" required>
          <el-input v-model="newFormInline.album" clearable placeholder="请输入专辑" />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="发行日期" prop="releaseTime" required>
          <el-date-picker v-model="newFormInline.releaseTime" type="date" placeholder="请选择发行日期"
            format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="曲风">
          <el-select v-model="newFormInline.style" placeholder="请选择曲风" class="w-full" clearable multiple>
            <el-option v-for="(item, index) in styleOptions" :key="index" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </re-col>

      <!-- 新增模式：音频上传 -->
      <re-col v-if="newFormInline.title === '新增'" :value="24" :xs="24" :sm="24">
        <el-form-item label="音频文件" prop="audioFile" required>
          <input type="file" accept="audio/*" @change="handleAudioChange" style="display:none" ref="audioInput" />
          <el-button @click="($refs.audioInput as HTMLInputElement).click()">选择音频文件</el-button>
          <span v-if="audioFileName" class="ml-2 text-sm text-gray-500">{{ audioFileName }}</span>
          <span v-if="audioDuration" class="ml-2 text-sm text-primary">时长: {{ audioDuration }}</span>
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
