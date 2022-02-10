import processing.sound.SoundFile;

SoundFile moveSound, captureSound, checkSound;
ArrayList<Piece> pieces, kings, kingKillers, killedPieces;
Piece current= null;
Cell[][] board;
PImage bg;
int turn, check, checkMate;
float w;
PFont roboto;
void setup() {
  size(720, 720);
  w= width/8f;
  bg= loadImage("board.png");
  roboto= createFont("Roboto-Medium.ttf", 20);
  textFont(roboto);
  textAlign(CENTER, CENTER);

  moveSound= new SoundFile(this, "sfx/move.mp3");
  captureSound= new SoundFile(this, "sfx/capture.mp3");
  checkSound= new SoundFile(this, "sfx/check.mp3");

  init();
}
void init() {
  turn= WHITE;
  check=0;
  checkMate= 0;
  pieces= new ArrayList<Piece>();
  kings= new ArrayList<Piece>();
  kingKillers= new ArrayList<Piece>();
  killedPieces= new ArrayList<Piece>();
  board= new Cell[8][8];
  for (int i=0; i<8; i++) {
    for (int j=0; j<8; j++) {
      board[i][j]= new Cell(i, j);
    }
  }
  pawns();
  knights();
  bishops();
  rooks();
  queens();
  kings();
}
void draw() {
  image(bg, 0, 0, width, height);
  updateGame();

  for (Cell[] col : board) {
    for (Cell cell : col) {
      cell.show();
    }
  }
  for (Piece piece : pieces) {
    piece.show();
  }
  if (current!=null) {
    current.show();
  }

  if (checkMate!=0) {
    textSize(w*2);
    fill(70, 200);
    rect(0, 0, width, height);
    fill(checkMate==BLACK ? 255 : 0);
    text(checkMate==BLACK ? "White" : "Black", w*4, w*3.2);
    textSize(w*1.1);
    text("Wins", w*4, w*4.7);
  }
}
int check() {
  int r= 0;
  kingKillers.clear();
  for (Piece king : kings) {
    for (Piece kp : pieces) {
      if (kp.clr==king.clr) continue;
      for (Move move : kp.possibleMoves) {
        if (kp.cellAfter(move)==king.cell) {
          r= king.clr;
          king.cell.state= DANGER;
          kingKillers.add(kp);
          break;
        }
      }
    }
    if (r!=0) break;
  }
  return r;
}
void keyPressed() {
  if (checkMate!=0 || key=='r' || key=='R') init();
}
void updateGame() {
  if (checkMate!=0) return;
  for (Piece piece : pieces) {
    piece.updateMoves();
  }
  int previous= check;

  check= check();

  if (check!=0 && kings.get((check+1)/2).possibleMoves.size()==0) checkMate= check;
  if (check!=0 && previous!=check) checkSound.play();
  for (Piece piece : pieces) {
    piece.enabled= piece.id==5 || check!=piece.clr;
  }
  enableKingHelpingPieces();
  disableKingUnblockingPieces();

  boolean we= false, be= false;
  for (Piece piece : pieces) {
    if (piece.possibleMoves.size()==0) continue;
    if (piece.clr==WHITE) we= true;
    else be= true;
  }

  if (!we && turn == WHITE) checkMate= WHITE;
  if (!be && turn == BLACK) checkMate= BLACK;
}
void mousePressed() {
  Cell cell= board[int(mouseX/w)][int(mouseY/w)];
  if (cell.piece==null || cell.piece.clr!=turn || !cell.piece.enabled) return;
  current= cell.piece;
}
void mouseDragged() {
  if (current==null) return;
  current.move();
}
void mouseReleased() {
  if (current==null) return;
  Move index= posToIndex(current.pos);
  if (!valid(index)) return;
  Cell next= board[index.x][index.y];
  Move move= Move.sub(next.pos, current.cell.pos);

  if (current.isValid(move)) {
    if (next.piece!=null) captureSound.play();
    else moveSound.play();
    current.applyMove(move);
    turn*=-1;
  } else current.reset();

  current= null;
  for (Cell[] col : board) {
    for (Cell cell : col) {
      cell.state= NONE;
    }
  }
}
