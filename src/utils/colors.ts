// prettier-ignore
const colorFromChar: string[] = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];

export const getColorForChar = (input: string, saturation: number = 63, lightness: number = 52) => {
  const index = colorFromChar.indexOf(input.toLowerCase().charAt(0));
  return `${index === -1 ? 0 : index * 10}, ${saturation}%, ${lightness}%`;
};

export const getColorForReleaseType = (type: string) => {
  const lowerType = type.toLowerCase();
  if (lowerType === 'release') {
    return '#27AE60';
  }
  
  if (lowerType === 'archived') {
    return "#000000"
  }

  return lowerType === 'beta' ? '#00a8ff' : '#ff0059';
};
