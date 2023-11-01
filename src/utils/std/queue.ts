/**
 * Queue data structure
 */
export class Queue<T> {
  constructor(private readonly queue: T[] = []) {}

  /**
   * Add an item to the queue
   * @param item
   */
  enqueue(item: T): void {
    this.queue.push(item);
  }

  /**
   * Remove an item from the queue top of the queue and return it
   */
  dequeue(): T | undefined {
    return this.queue.shift();
  }

  /**
   * Return the item at the top of the queue without removing it
   */
  peek(): T | undefined {
    return this.queue.length > 0 ? this.queue[0] : undefined;
  }
  
  isEmpty(): boolean {
    return this.queue.length === 0;
  }

  size(): number {
    return this.queue.length;
  }
}