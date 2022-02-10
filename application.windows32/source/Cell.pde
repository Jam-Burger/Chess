class Cell {
  int x, y, state;
  Piece piece= null;
  Move pos;
  Cell(int x, int y) {
    this.x= x;
    this.y= y;
    pos= new Move(x, y);
  }
  boolean isDangerFor(int clr0) {
    for (Piece piece0 : pieces) {
      for (Move move : piece0.killingMoves) {
        if (clr0 != piece0.clr && piece0.cellAfter(move)==this) return true;
      }
    }
    return false;
  }
  int containsPiece() {
    if (piece==null) return 0;
    return piece.clr;
  }
  void show() {
    noStroke();
    if (state==0) noFill();
    else if (state==CHOICE) fill(#D5E810, 100);
    else if (state==DANGER) fill(#FF1A1E, 130);
    else if (state==SELECT) fill(#1090CE, 130);

    rect(x*w, y*w, w, w);
  }
}
