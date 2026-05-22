interface FormItemProps {
  title: string;
  artistId?: number;
  artistName: string;
  gender: number;
  birth: Date;
  area: string;
  introduction: string;
  avatarFile?: File | null;
}
interface FormProps {
  formInline: FormItemProps;
}

export type { FormItemProps, FormProps };
