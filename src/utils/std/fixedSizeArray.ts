/**
 * Fixed size array
 */
export class FixedSizeArray<T> {
  private items: T[] = [];
  
  constructor(private readonly size: number) {}
  
  push(item: T) {
    if (this.items.length === this.size) {
      this.items.shift();
    }
    
    this.items.push(item);
  }
  
  getItems() {
    return this.items;
  }
  
  removeItem(item: T) {
    const index = this.items.indexOf(item);
    if (index > -1) {
      this.removeItemByIndex(index);
    }
  }
  
  removeItemByIndex(index: number) {
    if (index > -1) {
      this.items.splice(index, 1);
    }
  }
  
  clear() {
    this.items = [];
  }
  
  get length() {
    return this.items.length;
  }
}