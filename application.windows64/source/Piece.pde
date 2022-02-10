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
  Cell cellAfter(Move move) {
    if (valid(x+move.x, y+move.y)) return board[x+move.x][y+move.y];
    return null;
  }
  boolean isInDanger() {
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
  void applyMove(Move move) {
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
  void undoMove(Move move) {
    x-=move.x;
    y-=move.y;
    update();
  }
  boolean isValid(Move move) {
    for (Move m : possibleMoves) {
      if (m.equals(move)) return true;
    }
    return false;
  }
  void reset() {
    x= cell.x;
    y= cell.y;
    update();
  }
  void updateMoves() {
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
  void update() {
    pos= new PVector(x*w, y*w);
    cell.piece= null;
    cell= board[x][y];
    cell.piece= this;
  }
  void move() {
    pos.x= mouseX-w/2;
    pos.y= mouseY-w/2;
  }
  void show() {
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
