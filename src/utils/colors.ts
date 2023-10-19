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
