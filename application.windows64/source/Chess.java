import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.sound.SoundFile; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Chess extends PApplet {



SoundFile moveSound, captureSound, checkSound;
ArrayList<Piece> pieces, kings, kingKillers, killedPieces;
Piece current= null;
Cell[][] board;
PImage bg;
int turn, check, checkMate;
float w;
PFont roboto;
public void setup() {
  
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
public void init() {
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
public void draw() {
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
    text(checkMate==BLACK ? "White" : "Black", w*4, w*3.2f);
    textSize(w*1.1f);
    text("Wins", w*4, w*4.7f);
  }
}
public int check() {
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
public void keyPressed() {
  if (checkMate!=0 || key=='r' || key=='R') init();
}
public void updateGame() {
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
public void mousePressed() {
  Cell cell= board[PApplet.parseInt(mouseX/w)][PApplet.parseInt(mouseY/w)];
  if (cell.piece==null || cell.piece.clr!=turn || !cell.piece.enabled) return;
  current= cell.piece;
}
public void mouseDragged() {
  if (current==null) return;
  current.move();
}
public void mouseReleased() {
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
class Cell {
  int x, y, state;
  Piece piece= null;
  Move pos;
  Cell(int x, int y) {
    this.x= x;
    this.y= y;
    pos= new Move(x, y);
  }
  public boolean isDangerFor(int clr0) {
    for (Piece piece0 : pieces) {
      for (Move move : piece0.killingMoves) {
        if (clr0 != piece0.clr && piece0.cellAfter(move)==this) return true;
      }
    }
    return false;
  }
  public int containsPiece() {
    if (piece==null) return 0;
    return piece.clr;
  }
  public void show() {
    noStroke();
    if (state==0) noFill();
    else if (state==CHOICE) fill(0xffD5E810, 100);
    else if (state==DANGER) fill(0xffFF1A1E, 130);
    else if (state==SELECT) fill(0xff1090CE, 130);

    rect(x*w, y*w, w, w);
  }
}
static class Move {
  int x, y;
  Move(float x, float y) {
    this.x= PApplet.parseInt(x);
    this.y= PApplet.parseInt(y);
  }
  public static Move add(Move a, Move m) {
    return new Move(a.x+m.x, a.y+m.y);
  }
  public static Move sub(Move a, Move b) {
    return new Move(a.x-b.x, a.y-b.y);
  }
  public Move mult(int n) {
    return new Move(x*n, y*n);
  }
  public int mag() {
    if (x==0) return abs(y);
    else if (y==0) return abs(x);
    else if (x!=0 && y!=0 && abs(x)==abs(y)) return abs(x);
    return 0;
  }
  public Move setMag(int n) {
    return normalize().mult(n);
  }
  public Move normalize() {
    int x0= x==0 ? 0 : x/abs(x);
    int y0= y==0 ? 0 : y/abs(y);
    return new Move(x0, y0);
  }
  public boolean equals(Move other) {
    return this.x==other.x && this.y==other.y;
  }
  public Move flip() {
    return new Move(x, -y);
  }
}
final int PAWN= 0;
final int KNIGHT= 1;
final int BISHOP= 2;
final int ROOK= 3;
final int QUEEN= 4;
final int KING= 5;

final int NONE= 0;
final int CHOICE= 1;
final int DANGER= 2;
final int SELECT= 3;

final int BLACK= -1;
final int WHITE= +1;

public int imgId(int id) {
  switch(id) {
  case 0:
    return 5;
  case 1:
    return 3;
  case 2:
    return 2;
  case 3:
    return 4;
  case 4:
    return 1;
  case 5:
    return 0;
  }
  return -1;
}

public Move posToIndex(float x, float y) {
  return new Move((x+w/2)/w, (y+w/2)/w);
}
public Move posToIndex(PVector v) {
  return posToIndex(v.x, v.y);
}
public boolean valid(int x, int y) {
  return x>=0 && y>=0 && x<8 && y<8;
}
public boolean valid(Move v) {
  return v.x>=0 && v.y>=0 && v.x<8 && v.y<8;
}
Move[] pawnMoves= {
  new Move(0, -1), 
  new Move(0, -2), 
  new Move(-1, -1), 
  new Move(1, -1)
};
Move[] knightMoves= {
  new Move(1, 2), new Move(1, -2), 
  new Move(-1, 2), new Move(-1, -2), 
  new Move(2, 1), new Move(2, -1), 
  new Move(-2, 1), new Move(-2, -1)
};
Move[] bishopMoves={
  new Move(1, 1), 
  new Move(1, -1), 
  new Move(-1, 1), 
  new Move(-1, -1)
};
Move[] rookMoves={
  new Move(0, 1), 
  new Move(0, -1), 
  new Move(1, 0), 
  new Move(-1, 0)
};
Move[] queenMoves={
  new Move(1, 1), 
  new Move(1, -1), 
  new Move(-1, 1), 
  new Move(-1, -1), 
  new Move(0, 1), 
  new Move(0, -1), 
  new Move(1, 0), 
  new Move(-1, 0)
};
Move[] kingMoves= queenMoves;

Move[][] everyMoves= new Move[][] {
  pawnMoves, 
  knightMoves, 
  bishopMoves, 
  rookMoves, 
  queenMoves, 
  kingMoves
};
public void pawns() {
  for (int clr=BLACK; clr<=WHITE; clr+=2) {
    for (int x=0; x<=7; x++) {
      int y= (clr==BLACK) ? 1 : 6;
      pieces.add(new Piece(x, y, PAWN, clr));
    }
  }
}
public void knights() {
  pieces.add(new Piece(1, 0, KNIGHT, BLACK));
  pieces.add(new Piece(6, 0, KNIGHT, BLACK));
  pieces.add(new Piece(1, 7, KNIGHT, WHITE));
  pieces.add(new Piece(6, 7, KNIGHT, WHITE));
}
public void bishops() {
  pieces.add(new Piece(2, 0, BISHOP, BLACK));
  pieces.add(new Piece(5, 0, BISHOP, BLACK));
  pieces.add(new Piece(2, 7, BISHOP, WHITE));
  pieces.add(new Piece(5, 7, BISHOP, WHITE));
}
public void rooks() {
  pieces.add(new Piece(0, 0, ROOK, BLACK));
  pieces.add(new Piece(7, 0, ROOK, BLACK));
  pieces.add(new Piece(0, 7, ROOK, WHITE));
  pieces.add(new Piece(7, 7, ROOK, WHITE));
}
public void queens() {
  pieces.add(new Piece(3, 0, QUEEN, BLACK));
  pieces.add(new Piece(3, 7, QUEEN, WHITE));
}
public void kings() {
  kings.add(new Piece(4, 0, KING, BLACK));
  kings.add(new Piece(4, 7, KING, WHITE));

  pieces.add(kings.get(0));
  pieces.add(kings.get(1));
}
public void enableKingHelpingPieces() {
  if (check!=0) {
    Piece king= kings.get((check+1)/2);
    for (Piece hp : pieces) {
      if (hp.clr==-check || hp.id==KING) continue;
      ArrayList<Move> newMoves= new ArrayList<Move>();
      if (kingKillers.size()==1) {
        Cell c = kingKillers.get(0).cell;
        for (Move move : hp.possibleMoves) {
          if (hp.cellAfter(move)==c) {
            newMoves.add(move);
            hp.enabled= true;
            checkMate= 0;
            break;
          }
        }
      }
      for (Piece kp : kingKillers) {
        if (kp.id==BISHOP || kp.id==ROOK || kp.id==QUEEN) {
          Move pos= new Move(king.x-kp.x, king.y-kp.y);
          for (int n=1; n<=pos.mag(); n++) {
            Cell cell= kp.cellAfter(pos.setMag(n));
            if (cell.containsPiece()==0) {
              for (Move move : hp.possibleMoves) {
                if (hp.cellAfter(move)==cell) {
                  checkMate= 0;
                  hp.enabled= true;
                  newMoves.add(move);
                  break;
                }
              }
            }
          }
        }
      }
      hp.possibleMoves= newMoves;
    }
  }
}
public void disableKingUnblockingPieces() {
  // disabling king unblocking pieces
  for (Piece King : kings) {
    for (Piece kp : pieces) {
      if (kp.clr != King.clr && (kp.id==BISHOP || kp.id==ROOK || kp.id==QUEEN)) {
        Move move= new Move(King.x-kp.x, King.y - kp.y);
        if (((kp.id==BISHOP && abs(move.x)==abs(move.y) ) || (kp.id==ROOK && (move.x==0 || move.y==0))) || (kp.id==QUEEN && (move.x==0 || move.y==0 || abs(move.x)==abs(move.y)))) {
          ArrayList<Piece> blockingPieces = new ArrayList<Piece>();
          for (int n=1; n<move.mag(); n++) {
            Cell cell0= kp.cellAfter(move.setMag(n));
            if (cell0 != null && cell0.containsPiece() != 0) {
              blockingPieces.add(cell0.piece);
            }
          }
          if (blockingPieces.size()==1 && blockingPieces.get(0).clr==King.clr) {
            boolean valid= false;
            Piece bp= blockingPieces.get(0);
            Move moveToKillingPiece= new Move(kp.x - bp.x, kp.y - bp.y);
            Move moveToKing= new Move(King.x - bp.x, King.y - bp.y);

            ArrayList<Move> newMoves= new ArrayList<Move>();
            for (Move originalMove : bp.possibleMoves) {
              for (int m=1; m<moveToKing.mag(); m++) {
                if (originalMove.equals(moveToKing.setMag(m))) {
                  newMoves.add(moveToKing.setMag(m));
                  valid= true;
                }
              }
              for (int n=1; n<=moveToKillingPiece.mag(); n++) {
                if (originalMove.equals(moveToKillingPiece.setMag(n))) {
                  newMoves.add(moveToKillingPiece.setMag(n));
                  valid= true;
                }
              }
            }
            if (valid) bp.possibleMoves = newMoves;
            bp.enabled= valid;
          }
        }
      }
    }
  }
}
public void killPiece(Piece p) {
  for (int i=0; i<pieces.size(); i++) {
    if (pieces.get(i) == p) {
      pieces.remove(i);
      return;
    }
  }
}
class Piece {
  int x, y, clr, id;
  PVector pos;
  Cell cell;
  PImage img;
  ArrayList<Move> possibleMoves;
  ArrayList<Move> killingMoves;
  boolean used= false, enabled= true;
  Move[] allMoves;
  Piece(int x, int y, int id, int clr) {
    this.x= x;
    this.y= y;
    this.clr= clr;
    this.id= id;
    img= loadImage("pieces/" + (clr == WHITE ? "1" : "0") + id + ".png");

    pos= new PVector(x*w, y*w);
    cell= board[x][y];
    cell.piece= this;

    possibleMoves= new ArrayList<Move>();
    killingMoves= new ArrayList<Move>();
    
    allMoves= everyMoves[id];
  }
  public Cell cellAfter(Move move) {
    if (valid(x+move.x, y+move.y)) return board[x+move.x][y+move.y];
    return null;
  }
  public boolean isInDanger() {
    for (Piece piece : pieces) {
      for (Move move : piece.possibleMoves) {
        if (clr != piece.clr && piece.id!=0 && piece.x+move.x == x && piece.y+move.y == y) {
          return true;
        }
      }
      if (clr != piece.clr && piece.id==0 && piece.y-piece.clr == y && (piece.x+1==x || piece.x-1==x)) {
        return true;
      }
    }
    return false;
  }
  public void applyMove(Move move) {
    Cell next= cellAfter(move);
    if (next!=null && next.containsPiece() == -clr) {
      killPiece(next.piece);
      killedPieces.add(next.piece);
      next.piece= null;
      captureSound.play();
    }
    // castling
    if (id==5 && abs(move.x)==2) {
      Piece rook= (move.x>0) ? board[7][y].piece : board[0][y].piece;
      Move rookMove= (move.x>0) ? new Move(-2, 0) : new Move(3, 0);
      rook.applyMove(rookMove);
    }
    x+=move.x; 
    y+=move.y;
    update();
    used= true;
    if (id==PAWN && ((clr==BLACK&&y==7)||(clr==WHITE&&y==0))) {
      id= QUEEN;
      allMoves= everyMoves[QUEEN];
      img= loadImage("pieces/" + (clr == WHITE ? "1" : "0") + QUEEN + ".png");
    }
  }
  public void undoMove(Move move) {
    x-=move.x;
    y-=move.y;
    update();
  }
  public boolean isValid(Move move) {
    for (Move m : possibleMoves) {
      if (m.equals(move)) return true;
    }
    return false;
  }
  public void reset() {
    x= cell.x;
    y= cell.y;
    update();
  }
  public void updateMoves() {
    possibleMoves.clear();
    killingMoves.clear();
    if (id==PAWN) {
      Move m0= allMoves[0].mult(clr);
      Move m1= allMoves[1].mult(clr);

      if (cellAfter(m0)!=null && cellAfter(m0).containsPiece()==0) possibleMoves.add(m0);
      if (!used && cellAfter(m0).containsPiece()==0 && cellAfter(m1).containsPiece()==0) possibleMoves.add(m1);
      for (int i=2; i<=3; i++) {
        Move m= allMoves[i].mult(clr);
        if (cellAfter(m)!=null) {
          killingMoves.add(m);
          if (cellAfter(m).containsPiece()==-clr) {
            possibleMoves.add(m);
          }
        }
      }
    } else if (id==KNIGHT) {
      for (Move move : allMoves) {
        Cell next= cellAfter(move);
        if (next!=null && next.containsPiece()!=clr) {
          possibleMoves.add(move);
        }
      }
      killingMoves= possibleMoves;
    } else if (id==BISHOP || id==ROOK || id==QUEEN) {
      for (Move m : allMoves) {
        for (int n=1; n<8; n++) {
          Move move= m.setMag(n);
          Cell c= cellAfter(move);
          if (c==null) break;
          killingMoves.add(move);
          if (c.containsPiece()==clr) break;
          possibleMoves.add(move);
          if (c.containsPiece()==-clr) {
            if (c.piece.id==5 && cellAfter(m.setMag(n+1))!=null && cellAfter(m.setMag(n+1)).containsPiece()!=-clr) {
              killingMoves.add(m.setMag(n+1));
            }
            break;
          }
        }
      }
    } else if (id==KING) {
      for (Move move : allMoves) {
        Cell next= cellAfter(move);
        if (next==null) continue;
        killingMoves.add(move);
        if (!next.isDangerFor(clr) && next.containsPiece() != clr) possibleMoves.add(move);

        // adding castling moves
        if (!used && !isInDanger()) {
          for (Piece piece : pieces) {
            if (piece.id==ROOK && piece.clr==clr && !piece.used && !piece.isInDanger()) {
              // king side
              if (piece.x==7 && board[5][y].containsPiece()==0 && board[6][y].containsPiece()==0) {
                if (!board[5][y].isDangerFor(clr) && !board[6][y].isDangerFor(clr)) possibleMoves.add(new Move(2, 0));
              }
              // queen side
              if (piece.x==0 && board[1][y].containsPiece()==0 && board[2][y].containsPiece()==0 && board[3][y].containsPiece()==0) {
                if (!board[2][y].isDangerFor(clr) && !board[3][y].isDangerFor(clr)) possibleMoves.add(new Move(-2, 0));
              }
            }
          }
        }
      }
    }
  }
  public void update() {
    pos= new PVector(x*w, y*w);
    cell.piece= null;
    cell= board[x][y];
    cell.piece= this;
  }
  public void move() {
    pos.x= mouseX-w/2;
    pos.y= mouseY-w/2;
  }
  public void show() {
    if (this==current) {
      cell.state= SELECT;
      for (Move move : possibleMoves) {
        Cell choice= cellAfter(move);
        if (choice==null) continue;
        choice.state= CHOICE;
        if (choice.containsPiece()!=0) choice.state= DANGER;
      }
    }
    image(img, pos.x, pos.y, w, w);
  }
}
  public void settings() {  size(720, 720); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Chess" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
