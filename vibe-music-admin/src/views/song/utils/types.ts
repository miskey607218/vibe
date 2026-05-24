interface FormItemProps {
  title: string;
  artistId: number;
  artistName: string;
  artistList: Array<{ label: string; value: number }>;
  songId: number;
  songName: string;
  album: string;
  style: Array<string>;
  releaseTime: Date;
  duration: string;
  audioFile: File | null;
}
interface FormProps {
  formInline: FormItemProps;
}

export type { FormItemProps, FormProps };
