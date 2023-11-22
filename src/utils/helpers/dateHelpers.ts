import dayjs from "dayjs";
import relative from "dayjs/plugin/relativeTime";
dayjs.extend(relative);

export const StandardDateFormat = "DD MMMM YYYY";
export const StandardDateTimeFormat = "DD MMMM YYYY, HH:mm";

export const standardDate = (value: string | number) => {
  if (!value) return '';
  
  const day = typeof value === 'string' ? dayjs(value) : dayjs.unix(value);
  return day.format(StandardDateFormat);
}

export const standardDateTime = (value: string | number) => {
  if (!value) return '';

  const day = typeof value === 'string' ? dayjs(value) : dayjs.unix(value);
  return day.format(StandardDateTimeFormat);
}

export const timeFromNow = (value: string | number) => {
  if (!value) return 'Never';

  const day = typeof value === 'string' ? dayjs(value) : dayjs.unix(value);
  return day.fromNow(true) + ' ago';
}