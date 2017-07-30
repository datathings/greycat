namespace Helpers {

  export class Timer {

    private timerId: NodeJS.Timer;
    private start: number;
    private callback: () => void;
    private remaining: number;

    constructor(callback: () => void, delay: number) {
      this.callback = callback;
      this.remaining = delay;
      this.resume();
    }

    pause() {
      this.clear();
      this.remaining -= (new Date()).getTime() - this.start;
    }

    resume() {
      this.start = (new Date()).getTime();
      clearTimeout(this.timerId);
      this.timerId = setTimeout(this.callback, this.remaining);
    }

    clear() {
      clearTimeout(this.timerId);
    }
  }

}

export default Helpers;
