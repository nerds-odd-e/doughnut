const addDays = function (date: Date, days: number = 0): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

const formatDateToISO = (date: Date): string => {
    const padZero = (num: number): string => num.toString().padStart(2, '0');
  
    const year = date.getFullYear();
    const month = padZero(date.getMonth() + 1);
    const day = padZero(date.getDate());
  
    return `${year}-${month}-${day}`;
  }
  
  
export { addDays, formatDateToISO }