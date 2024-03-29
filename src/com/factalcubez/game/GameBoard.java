package com.factalcubez.game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;
import java.util.Stack;

public class GameBoard {

	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int UP = 2;
	public static final int DOWN = 3;

	public static final int ROWS = 4;
	public static final int COLS = 4;

	private final int startingTiles = 2;
	private Tile[][] board;
	// state
	private Stack<Tile[][]> undoStack;
	private Stack<Integer> scoreStack;

	private boolean dead;
	private boolean won;
	private BufferedImage gameBoard;
    private BufferedImage finalBoard;
	private int x;
	private int y;
	private int score= 0;
	private int scoreInCurrentStep= 0;
	private int highScore= 0;
	private Font scoreFont;

	private static int SPACING = 10;
	public static int BOARD_WIDTH = (COLS + 1) * SPACING + COLS * Tile.WIDTH;
	public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;

	private long elapsedMS;
	private long startTime;
	private boolean hasStarted;

	//saving
	private String	saveDataPath;
	private String fileName="SaveData";

	
	private int saveCount = 0;

	public GameBoard(int x, int y) {
		try {
			saveDataPath=GameBoard.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

		}
		catch (Exception e){
			e.printStackTrace();
		}
		scoreFont= Game.main.deriveFont(24f);
		this.x = x;
		this.y = y;
		board = new Tile[ROWS][COLS];
		gameBoard = new BufferedImage(BOARD_WIDTH,BOARD_HEIGHT,BufferedImage.TYPE_INT_RGB);
        finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
		undoStack = new Stack<Tile[][]>();
		scoreStack = new Stack<Integer>();

		loadHighScore();

		createBoardImage();

        start();
	}

	private void createSaveData(){
		try {
			File file =new File(saveDataPath, fileName);
			FileWriter output= new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(output);
			writer.write(""+0);
			//create fastest time
			writer.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	private  void loadHighScore(){
	try {
		File f = new File(saveDataPath, fileName);
		if (!f.isFile()){
			createSaveData();
		}
		BufferedReader reader= new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		highScore=Integer.parseInt(reader.readLine());
		//read fastest time
		reader.close();
	}
	catch (Exception e){
		e.printStackTrace();
	}
	}
	private  void setHighScore(){
		FileWriter output=null;

		try {
			File f = new File(saveDataPath,fileName);
			output= new FileWriter(f);
			BufferedWriter writer = new BufferedWriter(output);

			writer.write((""+highScore));
			writer.newLine();
			writer.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}


	public void reset(){
		board = new Tile[ROWS][COLS];
		start();
		dead = false;
		won = false;
		hasStarted = false;
		startTime = System.nanoTime();
		elapsedMS = 0;
		saveCount = 0;
	}

	private void start() {
		for (int i = 0; i < startingTiles; i++) {
			spawnRandom();
		}
	}

	private void createBoardImage() {
		Graphics2D g = (Graphics2D) gameBoard.getGraphics();
		g.setColor(Color.darkGray);
		g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
		g.setColor(Color.lightGray);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLS; col++) {
				int x = SPACING + SPACING * col + Tile.WIDTH * col;
				int y = SPACING + SPACING * row + Tile.HEIGHT * row;
				g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);
			}
		}
	}

	public void update() {
		saveCount++;
		if (saveCount >= 120) {
			saveCount = 0;
		}
		
		if (!won && !dead) {
			if (hasStarted) {
				elapsedMS = (System.nanoTime() - startTime) / 1000000;
			}
			else {
				startTime = System.nanoTime();
			}
		}

		checkKeys();

		if (score>highScore){
			highScore=score;
		}
		
		//check WIN
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLS; col++) {
				Tile current = board[row][col];
				if (current == null) continue;
				current.update();
				resetPosition(current, row, col);
				//tHuy edit
				if (current.getValue()==2048){
					won=true;
				}
			}
		}
	}

	public void render(Graphics2D g) {
		BufferedImage finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D) finalBoard.getGraphics();
		g2d.setColor(new Color(0, 0, 0, 0));
		g2d.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
		g2d.drawImage(gameBoard, 0, 0, null);

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLS; col++) {
				Tile current = board[row][col];
				if (current == null) continue;
				current.render(g2d);
			}
		}

		g.drawImage(finalBoard, x, y, null);
		g2d.dispose();

		g.setColor(Color.lightGray);
		g.setFont(scoreFont);
		g.drawString(""+score,30,40);
		g.setColor(Color.red);
		g.drawString("Best: "+highScore,Game.WIDTH - DrawUtils.getMessageWidth("Best: "+highScore, scoreFont,g)-20, 40);

		
	}

	private void resetPosition(Tile tile, int row, int col) {
		if (tile == null) return;

		int x = getTileX(col);
		int y = getTileY(row);

		int distX = tile.getX() - x;
		int distY = tile.getY() - y;

		if (Math.abs(distX) < Tile.SLIDE_SPEED) {
			tile.setX(tile.getX() - distX);
		}

		if (Math.abs(distY) < Tile.SLIDE_SPEED) {
			tile.setY(tile.getY() - distY);
		}

		if (distX < 0) {
			tile.setX(tile.getX() + Tile.SLIDE_SPEED);
		}
		if (distY < 0) {
			tile.setY(tile.getY() + Tile.SLIDE_SPEED);
		}
		if (distX > 0) {
			tile.setX(tile.getX() - Tile.SLIDE_SPEED);
		}
		if (distY > 0) {
			tile.setY(tile.getY() - Tile.SLIDE_SPEED);
		}
	}

	public int getTileX(int col) {
		return SPACING + col * Tile.WIDTH + col * SPACING;
	}

	public int getTileY(int row) {
		return SPACING + row * Tile.HEIGHT + row * SPACING;
	}

	private boolean checkOutOfBounds(Direction direction, int row, int col) {
		if (direction == Direction.LEFT) {
			return col < 0;
		}
		else if (direction == Direction.RIGHT) {
			return col > COLS - 1;
		}
		else if (direction == Direction.UP) {
			return row < 0;
		}
		else if (direction == Direction.DOWN) {
			return row > ROWS - 1;
		}
		return false;
	}

	private boolean move(int row, int col, int horizontalDirection, int verticalDirection, Direction direction) {
		boolean canMove = false;
		Tile current = board[row][col];
		if (current == null) return false;
		boolean move = true;
		int newCol = col;
		int newRow = row;
		while (move) {
			newCol += horizontalDirection;
			newRow += verticalDirection;
			if (checkOutOfBounds(direction, newRow, newCol)) break;
			if (board[newRow][newCol] == null) {
				board[newRow][newCol] = current;
				canMove = true;
				board[newRow - verticalDirection][newCol - horizontalDirection] = null;
				board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
			}
			//4 + 8 = 12
			else if (board[newRow][newCol].getValue() == current.getValue() && board[newRow][newCol].canCombine()) {
				board[newRow][newCol].setCanCombine(false);
				board[newRow][newCol].setValue(board[newRow][newCol].getValue() * 2);
				canMove = true;
				board[newRow - verticalDirection][newCol - horizontalDirection] = null;
				board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
				board[newRow][newCol].setCanCombine(true);
//				board[newRow][newCol].setCombineAnimation(true);
				score+=board[newRow][newCol].getValue();
				scoreInCurrentStep = board[newRow][newCol].getValue();
			}
			else {
				move = false;
			}
		}
		return canMove;
	}

	public void moveTiles(Direction direction) {
		boolean canMove = false;
		int horizontalDirection = 0;
		int verticalDirection = 0;

		if (direction == Direction.LEFT) {
			horizontalDirection = -1;
			for (int row = 0; row < ROWS; row++) {
				for (int col = 0; col < COLS; col++) {
					if (!canMove)
						canMove = move(row, col, horizontalDirection, verticalDirection, direction);
					else move(row, col, horizontalDirection, verticalDirection, direction);
				}
			}
		}
		else if (direction == Direction.RIGHT) {
			horizontalDirection = 1;
			for (int row = 0; row < ROWS; row++) {
				for (int col = COLS - 1; col >= 0; col--) {
					if (!canMove)
						canMove = move(row, col, horizontalDirection, verticalDirection, direction);
					else move(row, col, horizontalDirection, verticalDirection, direction);
				}
			}
		}
		else if (direction == Direction.UP) {
			verticalDirection = -1;
			for (int row = 0; row < ROWS; row++) {
				for (int col = 0; col < COLS; col++) {
					if (!canMove)
						canMove = move(row, col, horizontalDirection, verticalDirection, direction);
					else move(row, col, horizontalDirection, verticalDirection, direction);
				}
			}
		}
		else if (direction == Direction.DOWN) {
			verticalDirection = 1;
			for (int row = ROWS - 1; row >= 0; row--) {
				for (int col = 0; col < COLS; col++) {
					if (!canMove)
						canMove = move(row, col, horizontalDirection, verticalDirection, direction);
					else move(row, col, horizontalDirection, verticalDirection, direction);
				}
			}
		}
		else {
			System.out.println(direction + " is not a valid direction.");
		}

		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLS; col++) {
				Tile current = board[row][col];
				if (current == null) continue;
				current.setCanCombine(true);
			}
		}

		if (canMove) {
			spawnRandom();
		}
	}
	private void checkDead(){
		for (int row = 0; row<ROWS;row++){
			for (int col = 0; col< COLS; col++){
				if (board[row][col]==null)return;
				if (checkSurroundingTiles(row,col,board[row][col])){
					return;
				}
			}
		}
		dead=true;
		setHighScore();
	}



	private boolean checkSurroundingTiles(int row, int col, Tile tile) {
		if (row > 0) {
			Tile check = board[row - 1][col];
			if (check == null) return true;
			if (tile.getValue() == check.getValue()) return true;
		}
		if (row < ROWS - 1) {
			Tile check = board[row + 1][col];
			if (check == null) return true;
			if (tile.getValue() == check.getValue()) return true;
		}
		if (col > 0) {
			Tile check = board[row][col - 1];
			if (check == null) return true;
			if (tile.getValue() == check.getValue()) return true;
		}
		if (col < COLS - 1) {
			Tile check = board[row][col + 1];
			if (check == null) return true;
			if (tile.getValue() == check.getValue()) return true;
		}
		return false;
	}
	private void spawnRandom() {
		Random random = new Random();
		boolean notValid = true;

		while (notValid) {
			int location = random.nextInt(16);
			int row = location / ROWS;
			int col = location % COLS;
			Tile current = board[row][col];
			if (current == null) {
				int value = random.nextInt(10) < 9 ? 2 : 4;
				Tile tile = new Tile(value, getTileX(col), getTileY(row));
				board[row][col] = tile;
				notValid = false;
			}
		}
	}

	private void checkKeys() {
		// left keypress
		if (Keyboard.typed(KeyEvent.VK_LEFT)) {
			saveState();
			moveTiles(Direction.LEFT);

			if (!hasStarted)
				hasStarted = true;
		}
		// right keypress
		if (Keyboard.typed(KeyEvent.VK_RIGHT)) {
			saveState();
			moveTiles(Direction.RIGHT);

			if (!hasStarted)
				hasStarted = true;
		}
		// up keypress
		if (Keyboard.typed(KeyEvent.VK_UP)) {
			saveState();
			moveTiles(Direction.UP);

			if (!hasStarted)
				hasStarted = true;
		}
		// down keypress
		if(Keyboard.typed(KeyEvent.VK_DOWN)){
			saveState();
			moveTiles(Direction.DOWN);

            if(!hasStarted) hasStarted = true;
		}
		// U keypress
		if(Keyboard.typed(KeyEvent.VK_U)){
			if(!undoStack.isEmpty()) {
				board = undoStack.pop();
			}
			if(!scoreStack.isEmpty()) {
				setScore(scoreStack.pop()); 
			}
			System.out.println(getCurrentScore());
		}
		// space keypress
		if(Keyboard.typed(KeyEvent.VK_SPACE)){ 
			reset(); 
			// fix bug: https://github.com/tmhuyy/java-2048/issues/6
			setScore(0);
		}
	}


	private void saveState() {
		Tile[][] currentState = new Tile[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (board[i][j] != null) {
					currentState[i][j] = new Tile(board[i][j].getValue(), board[i][j].getX(), board[i][j].getY());
				}
			}
		}
		// 12
		
		// 2 2 -> 4 -> total score : 16 
		// [12, 4]
		scoreStack.push(getCurrentScore()); // score in current step
		undoStack.push(currentState);
	}
	// fix bug: https://github.com/tmhuyy/java-2048/issues/6
	public void setScore(int score)
	{
		this.score = score;
	}

	public int getCurrentScore(){
		return this.score;
	}
}
