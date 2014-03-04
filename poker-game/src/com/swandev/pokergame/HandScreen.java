package com.swandev.pokergame;

import java.util.Map;

import lombok.Getter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.google.common.collect.Maps;
import com.swandev.pokergame.HandScreen.HandRenderer.CardImage;
import com.swandev.swangame.screen.SwanScreen;
import com.swandev.swangame.socket.EventEmitter;

public class HandScreen extends SwanScreen {
	//*** Layout Coordinates ***//
	//Use the pixels per unit to define a grid and orient
	//all the elements of the screen based on that grid.
	private static final float CAMERA_WIDTH = 15f;  //how many boxes wide the screen is
	private static final float CAMERA_HEIGHT = 10f; //how many boxes high the screen is
	private float ppuX; // pixels per unit on the X axis
	private float ppuY; // pixels per unit on the Y axis
	
	//orient the cards in the players hand
	private static final float CARD_WIDTH = 4f;
	private static final float CARD_HEIGHT = 6f;	
	private static final float CARD1_ORIGIN_X = 5f;
	private static final float CARD1_ORIGIN_Y = 3f;
	private static final float CARD2_ORIGIN_X = 9f;
	private static final float CARD2_ORIGIN_Y = 3f;
	
	//orient the table of buttons for betting/folding
	private static final float BUTTON_WIDTH = 3f;
	private static final float BUTTON_HEIGHT = 1f;
	private static final float BUTTON_PADDING_LEFT = 1f;
	private static final float BUTTON_PADDING_TOP = 1f;
	
	//orient the text boxes which show the amount of $$ owned and bet
	private static final float MONEY_TEXT_WIDTH = 2f;
	private static final float MONEY_TEXT_HEIGHT = 1f;
	private static final float MONEY_TABLE_PADDING_RIGHT = 0.5f;
	private static final float MONEY_TABLE_PADDING_BOTTOM = 1f;
	
	//These labels are members so we can dynamically change their values
	//without looking them up in the stage
	private static Label cashLabel;
	private static Label betLabel;
	private static Label callLabel;
	
	//The hand is a member so we can dynamically change the values and the
	//orientation (face-up v. face-down) of the cards
	private HandRenderer myHand;
	
	//The action buttons (Raise, Check, Call, Fold, All-In) are members so
	//we can dynamically disable/enable them based on the current chip/call/bet values
	private TextButton raiseButton;
	private TextButton allInButton;
	private TextButton callButton;
	private TextButton checkButton;
	private TextButton foldButton;
	
	private PokerGame game;
	
	private final Stage stage;
	private PlayerState state;
	private Image backgroundImage;
	
	/** Textures **/
	private Map<Integer, TextureRegion> cardTextureMap = Maps.newHashMap();
	
	/** Animations **/
	//private Animation rollLeftAnimation;
	//private Animation rollRightAnimation;

	private int width;
	private int height;	
	
	public void setSize(int w, int h){
		this.width = w;
		this.height = h;
		ppuX = (float)width / CAMERA_WIDTH;
		ppuY = (float)height / CAMERA_HEIGHT;
		backgroundImage.setWidth(width);
		backgroundImage.setHeight(height);
	}
	
	public HandScreen(PokerGame game){
		super(game.getSocketIO());
		this.game = game;
		this.cardTextureMap = PokerLib.getCardTextures();
		
		this.stage = new Stage(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, game.getSpriteBatch());
		this.state = new PlayerState();
		ppuX = (float)Gdx.graphics.getWidth() / CAMERA_WIDTH;
		ppuY = (float)Gdx.graphics.getHeight() / CAMERA_HEIGHT;
		
		//fake being dealt two cards
		state.card1 = 101;
		state.card2 = 201;
		myHand = new HandRenderer(state);
		
		final Skin skin = game.getAssets().getSkin();
		
		//Build the elements of the stage as seen on the client screen
		//Note: Order is important here! The order in which we add the elements
		//is the order in which they will be rendered; this only *really* matters
		//for the background since it needs to be behind everything else, but also
		//determines who's in front in some weird resizing cases.
		buildBackground(skin);
		buildCards(skin);
		buildMoneyText(skin);
		buildButtonTable(skin);	
		
		//This call should be made at the end of a response to a "Your Turn" message, after
		//changing the PlayerState appropriately.
		enableLegalActionButtons();
	}
	
	@Override
	protected void registerEvents() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void unregisterEvents(EventEmitter eventEmitter) {
		// TODO Auto-generated method stub
		
	}
	
	private void buildBackground(Skin skin){
		//Adds a background texture to the stage
		backgroundImage = new Image(new TextureRegion(new Texture(Gdx.files.internal("images/background.png"))));
		backgroundImage.setX(0);
		backgroundImage.setY(0);
		backgroundImage.setWidth(width);
		backgroundImage.setHeight(height);
		backgroundImage.setFillParent(true);
		stage.addActor(backgroundImage);
	}
	
	private void buildCards(Skin skin){
		//The hand renderer takes care of the actual images that the cards display; but
		//here we position the cards and add them to the renderer.
		
		//Note: If you want to overlap the cards because you're cool like that, just change the
		//origins such that they overlap and the one added second (card2) is "in front" of the first one.
		CardImage card1Image = myHand.getCard1();
		card1Image.setX(CARD1_ORIGIN_X*ppuX);
		card1Image.setY(CARD1_ORIGIN_Y*ppuY);
		card1Image.setWidth(CARD_WIDTH*ppuX);
		card1Image.setHeight(CARD_HEIGHT*ppuY);
		stage.addActor(card1Image);
		
		CardImage card2Image = myHand.getCard2();
		card2Image.setX(CARD2_ORIGIN_X*ppuX);
		card2Image.setY(CARD2_ORIGIN_Y*ppuY);
		card2Image.setWidth(CARD_WIDTH*ppuX);
		card2Image.setHeight(CARD_HEIGHT*ppuY);
		stage.addActor(card2Image);
	}
	
	private void buildMoneyText(Skin skin){
		//These labels appear at the bottom of the screen in the form of:
		//Call: XXXXX   Cash: XXXXX   Bet: XXXX
		//The second of each pair is a member of the Screen and is updated along with the PlayerState.
		Table moneyTextTable = new Table(skin);
		
		moneyTextTable.defaults().width(MONEY_TEXT_WIDTH*ppuX);
		moneyTextTable.defaults().height(MONEY_TEXT_HEIGHT*ppuY);
		moneyTextTable.bottom().right();
		moneyTextTable.padRight(MONEY_TABLE_PADDING_RIGHT*ppuX).padBottom(MONEY_TABLE_PADDING_BOTTOM*ppuY);

		Label callText = new Label("Call:", skin);
		callLabel = new Label(new Integer(state.callValue).toString(), skin);
		moneyTextTable.add(callText);
		moneyTextTable.add(callLabel).padRight(MONEY_TABLE_PADDING_RIGHT*ppuX);
		
		Label cashText = new Label("Cash:", skin);
		cashLabel = new Label(new Integer(state.chipValue).toString(), skin);
		moneyTextTable.add(cashText);
		moneyTextTable.add(cashLabel).padRight(MONEY_TABLE_PADDING_RIGHT*ppuX);
		
		Label betText = new Label("Bet:", skin);
		betLabel = new Label(new Integer(state.betValue).toString(), skin);
		moneyTextTable.add(betText);
		moneyTextTable.add(betLabel);	
		
		moneyTextTable.setFillParent(true);
		
		stage.addActor(moneyTextTable);	
	}
	
	private void buildButtonTable(Skin skin){
		//Adds a table of action buttons in the top-left corner of the screen
		Table buttonTable = new Table(skin);

		buttonTable.defaults().width(BUTTON_WIDTH*ppuX);
		buttonTable.defaults().height(BUTTON_HEIGHT*ppuY);
		buttonTable.top().left();
		buttonTable.padLeft(BUTTON_PADDING_LEFT*ppuX).padTop(BUTTON_PADDING_TOP*ppuY);
		
		//Note that this section should be deleted completely once we are dealt cards from the dealer
		/******* BEGIN NON_SUBMIT CODE SECTION ******/
		/*Next Card Button increments the card value of both cards*/
		TextButton nextCardButton = new TextButton("Next Card", skin);
		nextCardButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				state.card1 = incrementCard(state.card1);
				state.card2 = incrementCard(state.card2);
			}
		});
		buttonTable.add(nextCardButton);
		buttonTable.row();
		
		/*Next Suit Button cycles each card through the suits*/
		TextButton nextSuitButton = new TextButton("Next Suit", skin);
		nextSuitButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				state.card1 = incrementSuit(state.card1);
				state.card2 = incrementSuit(state.card2);
			}
		});
		buttonTable.add(nextSuitButton);
		buttonTable.row();
		/******* END NON_SUBMIT CODE SECTION ******/
		
		/* All-In Button Requests a bet which is equal to the total cash the player owns*/
		allInButton = new TextButton("All In!", skin);
		allInButton.setColor(Color.RED);
		allInButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				requestBet(state.chipValue);
			}
		});
		buttonTable.add(allInButton);
		buttonTable.row();
		
		/* Raise-1000 Button Requests a bet which 1000 more than the call value - the bet value*/
		raiseButton = new TextButton("Raise $1000", skin);
		raiseButton.setColor(Color.BLUE);
		raiseButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				requestBet(state.callValue + 1000 - state.betValue);
			}
		});
		buttonTable.add(raiseButton);
		buttonTable.row();
		
		/* Call Button Requests a bet which makes up the difference between the current bet and the call value*/
		callButton = new TextButton("Call", skin);
		callButton.setColor(Color.GREEN);
		callButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				requestBet(state.callValue - state.betValue);
			}
		});
		buttonTable.add(callButton);
		buttonTable.row();
		
		/* Check Button Requests a bet of 0, signaling to move on to the next player*/
		checkButton = new TextButton("Check", skin);
		checkButton.setColor(Color.YELLOW);
		checkButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				requestBet(0);
			}
		});
		buttonTable.add(checkButton);
		buttonTable.row();
		
		/* Fold Button Requests a bet of -1, symbolizing a user folding the current hand.*/
		foldButton = new TextButton("Fold", skin);
		foldButton.setColor(Color.GRAY);
		foldButton.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor){
				requestBet(-1);
			}
		});
		buttonTable.add(foldButton);
		buttonTable.row();
		
		buttonTable.setFillParent(true);

		stage.addActor(buttonTable);
	}
	
	private int incrementCard(int cardID){
		int ret = cardID + 1;
		if (ret % 100 > PokerLib.MAX_CARD){
			ret -= PokerLib.MAX_CARD;
		}
		return ret;
	}
	
	private int incrementSuit(int cardID){
		int ret = cardID + 100;
		if (ret - PokerLib.MAX_CARD > PokerLib.MAX_SUIT){
			ret -= PokerLib.MAX_SUIT;
		}
		return ret;
	}
	
	private void disableActionButtons(){
		//disables all the action buttons; should be called after a request is sent
		//and again once the acknowledge comes back
		raiseButton.setDisabled(true);
		allInButton.setDisabled(true);
		callButton.setDisabled(true);
		checkButton.setDisabled(true);
		foldButton.setDisabled(true);
	}
	
	private void enableLegalActionButtons(){
		//Based on the current PlayerState (call/chip/bet values), only some actions
		//are legal to process. Only enable those buttons which are legal!
		disableActionButtons(); //start by disabling all buttons
		
		//now re-enable only the legal ones
		foldButton.setDisabled(false); //you can always fold
		if (state.betValue < state.callValue){
			callButton.setDisabled(false); //you can only call if you haven't bet up to the call value
		} else {
			checkButton.setDisabled(false); //otherwise you will be able to check
		}
		if (state.chipValue > 0){
			allInButton.setDisabled(false);
		}
		if (state.chipValue > state.callValue + 1000){
			//all raises are exactly $1000 for now. Once that
			//changes, we should check for some minimum amount
			//a player is allowed to raise.
			raiseButton.setDisabled(false);
		}
	}
	
	private void requestBet(int betValue){
		//send the request to the server
		if (betValue == -1){
			//TODO: send a FOLD_REQUEST
			return;
		} else { 
			//TODO: send a BET_REQUEST
		}
		
		//disable the buttons while you wait for the ack; the valid ones will be re-enabled on a YOUR_TURN
		//method or in response to an INVALID_ACTION call (shouldn't happen if the buttons were enabled properly)
		disableActionButtons();
		
		//END OF FUNCTION (once the socket stuff is integrated)
		
		//BEGIN OF HACK
		
		//These steps should happen in response to the server acknowledging a bet action, and only
		//set the chipValue, betValue, and callValue to whatever the server said they should be.
		state.chipValue -= betValue;
		state.betValue += betValue;
		state.callValue = state.betValue;
		
		betLabel.setText(new Integer(state.betValue).toString());
		cashLabel.setText(new Integer(state.chipValue).toString());
		callLabel.setText(new Integer(state.callValue).toString());
		
		//Now re-enable the action buttons; this should happen ONLY in response to a YOUR_TURN or INVALID_ACTION
		//message from the server
		enableLegalActionButtons();
		
		//END OF HACK
	}
	
	public void render(float delta){
		//Minimal action taken inside the render loop
		super.render(delta);
		stage.draw();
		stage.act(delta);
	}
	
	public class HandRenderer {		
		//Used to populate the cards with the images to be rendered.
		//Since your hand is displayed on a screen that is likely somewhat visible
		//to other players, we want to hide the cards unless you click and drag upwards
		//on them (just like the pros do when they keep their cards down and lift them in
		//private).
		//Positioning the cards and adding them to the stage is handled above.
		private float startY;
		
		@Getter
		private CardImage card1;
		
		@Getter
		private CardImage card2;
		
		private final PlayerState state;
		
		public HandRenderer(PlayerState s){
			card1 = new CardImage();
			card2 = new CardImage();
			setCardDrawables(PokerLib.CARD_BACK, PokerLib.CARD_BACK);
			state = s;
			startY = -1f;
		}
		
		void setCardDrawables(int card1Value, int card2Value){
			card1.setDrawable(new TextureRegionDrawable(cardTextureMap.get(card1Value)));
			card2.setDrawable(new TextureRegionDrawable(cardTextureMap.get(card2Value)));
		}
		
		public class CardImage extends Image{			
			public CardImage(){
				super();
				
				addListener(new DragListener(){
					public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
						//When they first touch down on the card image, record the Y-coordinate
						startY = y;
						return true;
					}
					
					public void touchDragged(InputEvent event, float x, float y, int pointer){
						//If they drag along the card and the Y-coordinate increases, reveal the cards
						if (y > startY && startY > 0){
							setCardDrawables(state.card1, state.card2);
						}
					}
					
					public void touchUp(InputEvent event, float x, float y, int pointer, int button){
						//When they release the touch, turn the cards back down
						setCardDrawables(PokerLib.CARD_BACK, PokerLib.CARD_BACK);
						startY = -1f;
					}
					
				});
			}
		}
	} //End of HandRenderer class

	@Override
	public void resize(int width, int height) {
		stage.setViewport(width, height, true);
	}

	@Override
	public void show() {
		super.show();
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void hide() {
		super.hide();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

}
