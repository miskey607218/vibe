interface FormItemProps {
  title: string;
  artistId?: number;
  artistName: string;
  gender: number;
  birth: Date;
  area: string;
  introduction: string;
}
interface FormProps {
  formInline: FormItemProps;
}

export type { FormItemProps, FormProps };
