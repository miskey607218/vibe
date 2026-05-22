<script setup lang="ts">
import { ref, watch } from "vue";
import ReCol from "@/components/ReCol";
import { formRules } from "../utils/rule";
import { FormProps } from "../utils/types";

const props = withDefaults(defineProps<FormProps>(), {
  formInline: () => ({
    title: "新增",
    artistId: 0,
    artistName: "",
    gender: 0,
    birth: null,
    area: "",
    introduction: "",
    avatarFile: null
  })
});

const genderOptions = [
  { value: 0, label: "男歌手" },
  { value: 1, label: "女歌手" },
  { value: 2, label: "组合/乐队" }
];

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);
const avatarFileName = ref("");

watch(() => props.formInline, (val) => {
  newFormInline.value = val;
}, { immediate: true, deep: true });

function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    newFormInline.value.avatarFile = input.files[0] as any;
    avatarFileName.value = input.files[0].name;
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
      <re-col v-if="newFormInline.title === '修改'" :value="12" :xs="24" :sm="24">
        <el-form-item label="歌手编号">
          <el-input :model-value="newFormInline.artistId" disabled />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="歌手" prop="artistName" required>
          <el-input v-model="newFormInline.artistName" clearable placeholder="请输入歌手" />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="类型">
          <el-select v-model="newFormInline.gender" placeholder="请选择类型" class="w-full" clearable>
            <el-option v-for="(item, index) in genderOptions" :key="index" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="生日">
          <el-date-picker v-model="newFormInline.birth" type="date" placeholder="请选择生日"
            format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="国籍">
          <el-input v-model="newFormInline.area" clearable placeholder="请输入国籍" />
        </el-form-item>
      </re-col>

      <re-col :value="24" :xs="24" :sm="24">
        <el-form-item label="头像">
          <input type="file" accept="image/*" @change="handleAvatarChange" style="display:none" ref="avatarInput" />
          <el-button @click="($refs.avatarInput as HTMLInputElement).click()">选择头像</el-button>
          <span v-if="avatarFileName" class="ml-2 text-sm text-gray-500">{{ avatarFileName }}</span>
        </el-form-item>
      </re-col>

      <re-col>
        <el-form-item label="简介">
          <el-input v-model="newFormInline.introduction" placeholder="请输入简介" type="textarea"
            :autosize="{ minRows: 4, maxRows: 10 }" />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
