package com.swandev.poker;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.swandev.poker.PokerGameScreen.PokerRound;

public class PokerTable {
	PokerRound round;
	@Getter
	int callValue;
	PokerPot pot = new PokerPot();
	Deck deck = new Deck();
	@Getter
	List<Card> tableCards = Lists.newArrayList();
	List<PlayerStats> players;
	int dealer;
	int currentPlayer;
	int numChecksOrFoldsRequiredToAdvanceRounds;
	final PokerGameScreen pokerGameScreen;

	public PokerTable(PokerGameScreen pokerGameScreen, List<PlayerStats> players) {
		this.pokerGameScreen = pokerGameScreen;
		this.players = players;
	}

	public void newHand() {
		Gdx.app.log("poker", "Starting a new round of poker!");
		callValue = 0;
		deck.reset();
		round = PokerRound.PREFLOP;
		pot.reset();
		for (PlayerStats player : players) {
			player.resetBetweenRounds();
		}
		deck.shuffle();
		tableCards = deck.dealTable();
		for (PlayerStats player : players) {
			if (player.isAlive()) {
				deck.deal(player);
				Gdx.app.log("poker", "Dealt " + player.getPrivateCards() + " to player " + player.getName());
				player.setHand(getBestHand(player));
				final List<Integer> cardPictureValues = Lists.newArrayList();
				for (Card card : player.getPrivateCards()) {
					cardPictureValues.add(card.getImageNumber());
				}
				pokerGameScreen.getSocketIO().swanEmit(PokerLib.DEAL_HAND, player.getName(), cardPictureValues, 0, player.getMoney(), 0);
			}
		}
		numChecksOrFoldsRequiredToAdvanceRounds = getNumRemainingPlayersInRound();
		dealer = nextUnfoldedAlivePlayer(dealer);
		currentPlayer = nextUnfoldedAlivePlayer(dealer);
		pokerGameScreen.uiForPreFlop();
		PlayerStats playerStats = players.get(currentPlayer);
		pokerGameScreen.getSocketIO().swanEmit(PokerLib.YOUR_TURN, playerStats.getName(), playerStats.getBet(), playerStats.getMoney(), callValue);
	}

	private int nextUnfoldedAlivePlayer(int playerNumber) {
		int nextUnfoldedAlivePlayer = (playerNumber + 1) % players.size();
		while (!players.get(nextUnfoldedAlivePlayer).isAlive() || players.get(nextUnfoldedAlivePlayer).isFolded()) {
			nextUnfoldedAlivePlayer = (nextUnfoldedAlivePlayer + 1) % players.size();
		}
		return nextUnfoldedAlivePlayer;
	}

	private PokerHand getBestHand(PlayerStats player) {
		List<Card> combinedHand = Lists.newArrayList(player.getPrivateCards());
		combinedHand.addAll(getTableCards());
		return PokerHand.getBestHandFromSeven(combinedHand);
	}

	private int getNumRemainingPlayersInRound() {
		int remainingPlayers = 0;
		for (PlayerStats player : players) {
			if (player.isAlive() && !player.isFolded()) {
				remainingPlayers++;
			}
		}
		return remainingPlayers;
	}

	public void foldPlayer(PlayerStats player) {
		player.setFolded(true);
		numChecksOrFoldsRequiredToAdvanceRounds--;
		nextPlayer();
	}

	public void betPlayer(PlayerStats currentPlayer, int amount) {
		if (amount == PokerLib.BET_CHECK) {
			numChecksOrFoldsRequiredToAdvanceRounds--;
		} else {
			numChecksOrFoldsRequiredToAdvanceRounds = Integer.MAX_VALUE; // this condition can't happen anymore
			currentPlayer.placeBet(amount, pot);
			callValue = Math.max(callValue, currentPlayer.getBet());
		}
		pokerGameScreen.getSocketIO().swanEmit(PokerLib.ACTION_ACKNOWLEDGE, currentPlayer.getName(), currentPlayer.getBet(), currentPlayer.getMoney(), callValue);
		nextPlayer();
	}

	private void nextPlayer() {
		if (getNumRemainingPlayersInRound() == 1) {
			endHand();
		} else if (shouldAdvanceRounds()) {
			nextRound();
		} else {
			currentPlayer = nextUnfoldedAlivePlayer(currentPlayer);
			PlayerStats playerStats = players.get(currentPlayer);
			pokerGameScreen.getSocketIO().swanEmit(PokerLib.YOUR_TURN, playerStats.getName(), playerStats.getBet(), playerStats.getMoney(), callValue);
		}
	}

	private boolean shouldAdvanceRounds() {
		// If everyone has checked (ie call value is 0 and the player who just played was last alive closest to dealer)
		boolean shouldAdvance = false;
		if (numChecksOrFoldsRequiredToAdvanceRounds == 0) {
			Gdx.app.log("POKER", "Advancing rounds because everyone has checked or folded");
			shouldAdvance = true;
		} else {
			// If everyone alive still in has bet the same (non-zero) amount, the round should end
			List<Integer> bets = Lists.newArrayList();
			for (PlayerStats player : players) {
				if (player.isAlive() && !player.isFolded() && player.getBet() > 0) {
					bets.add(player.getBet());
				}
			}
			if (bets.size() == getNumRemainingPlayersInRound() && Sets.newHashSet(bets).size() == 1) {
				shouldAdvance = true;
			}
		}
		return shouldAdvance;
	}

	private void nextRound() {
		if (round == PokerRound.RIVER) {
			endHand();
		} else {
			round = PokerRound.values()[round.ordinal() + 1];
			Gdx.app.log("POKER", "Advancing to round " + round);
			pokerGameScreen.uiForDrawCards(round);
			for (PlayerStats player : players) {
				player.clearBet();
			}
			callValue = 0;
			numChecksOrFoldsRequiredToAdvanceRounds = getNumRemainingPlayersInRound();
			currentPlayer = nextUnfoldedAlivePlayer(dealer);
			PlayerStats playerStats = players.get(currentPlayer);
			pokerGameScreen.getSocketIO().swanEmit(PokerLib.YOUR_TURN, playerStats.getName(), playerStats.getBet(), playerStats.getMoney(), callValue);
		}
	}

	public void endHand() {
		final List<PlayerStats> showdownPlayers = Lists.newArrayList();
		for (PlayerStats player : players) {
			if (player.isAlive() && !player.isFolded()) {
				showdownPlayers.add(player);
			}
		}
		Collections.sort(showdownPlayers, new Comparator<PlayerStats>() {

			@Override
			public int compare(PlayerStats o1, PlayerStats o2) {
				return o1.getHand().compareTo(o2.getHand());
			}
		});
		Collections.reverse(showdownPlayers);
		List<PlayerStats> winners = Lists.newArrayList();
		int i = 0;
		while (i < showdownPlayers.size() && showdownPlayers.get(i).getHand().equals(showdownPlayers.get(0).getHand())) {
			winners.add(showdownPlayers.get(i));
			i++;
		}
		pot.payout(winners);
		Gdx.app.log("poker", "Winning hands " + showdownPlayers.get(0).getHand());
		for (PlayerStats player : players) {
			player.setAlive(player.getMoney() > 0);
			pokerGameScreen.getSocketIO().swanEmit(PokerLib.HAND_COMPLETE, player.getName(), 0, player.getMoney(), 0, winners.contains(player));
		}
		// TODO: check for GameOver condition (1 alive player)
		pokerGameScreen.uiBetweenHands();
	}
}