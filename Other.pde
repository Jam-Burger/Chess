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

int imgId(int id) {
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

Move posToIndex(float x, float y) {
  return new Move((x+w/2)/w, (y+w/2)/w);
}
Move posToIndex(PVector v) {
  return posToIndex(v.x, v.y);
}
boolean valid(int x, int y) {
  return x>=0 && y>=0 && x<8 && y<8;
}
boolean valid(Move v) {
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
void pawns() {
  for (int clr=BLACK; clr<=WHITE; clr+=2) {
    for (int x=0; x<=7; x++) {
      int y= (clr==BLACK) ? 1 : 6;
      pieces.add(new Piece(x, y, PAWN, clr));
    }
  }
}
void knights() {
  pieces.add(new Piece(1, 0, KNIGHT, BLACK));
  pieces.add(new Piece(6, 0, KNIGHT, BLACK));
  pieces.add(new Piece(1, 7, KNIGHT, WHITE));
  pieces.add(new Piece(6, 7, KNIGHT, WHITE));
}
void bishops() {
  pieces.add(new Piece(2, 0, BISHOP, BLACK));
  pieces.add(new Piece(5, 0, BISHOP, BLACK));
  pieces.add(new Piece(2, 7, BISHOP, WHITE));
  pieces.add(new Piece(5, 7, BISHOP, WHITE));
}
void rooks() {
  pieces.add(new Piece(0, 0, ROOK, BLACK));
  pieces.add(new Piece(7, 0, ROOK, BLACK));
  pieces.add(new Piece(0, 7, ROOK, WHITE));
  pieces.add(new Piece(7, 7, ROOK, WHITE));
}
void queens() {
  pieces.add(new Piece(3, 0, QUEEN, BLACK));
  pieces.add(new Piece(3, 7, QUEEN, WHITE));
}
void kings() {
  kings.add(new Piece(4, 0, KING, BLACK));
  kings.add(new Piece(4, 7, KING, WHITE));

  pieces.add(kings.get(0));
  pieces.add(kings.get(1));
}
void enableKingHelpingPieces() {
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
void disableKingUnblockingPieces() {
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
void killPiece(Piece p) {
  for (int i=0; i<pieces.size(); i++) {
    if (pieces.get(i) == p) {
      pieces.remove(i);
      return;
    }
  }
}
