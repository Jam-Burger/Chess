static class Move {
  int x, y;
  Move(float x, float y) {
    this.x= int(x);
    this.y= int(y);
  }
  static Move add(Move a, Move m) {
    return new Move(a.x+m.x, a.y+m.y);
  }
  static Move sub(Move a, Move b) {
    return new Move(a.x-b.x, a.y-b.y);
  }
  Move mult(int n) {
    return new Move(x*n, y*n);
  }
  int mag() {
    if (x==0) return abs(y);
    else if (y==0) return abs(x);
    else if (x!=0 && y!=0 && abs(x)==abs(y)) return abs(x);
    return 0;
  }
  Move setMag(int n) {
    return normalize().mult(n);
  }
  Move normalize() {
    int x0= x==0 ? 0 : x/abs(x);
    int y0= y==0 ? 0 : y/abs(y);
    return new Move(x0, y0);
  }
  boolean equals(Move other) {
    return this.x==other.x && this.y==other.y;
  }
  Move flip() {
    return new Move(x, -y);
  }
}
