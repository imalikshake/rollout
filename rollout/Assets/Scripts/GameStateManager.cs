﻿using UnityEngine;
using System.Collections;
using System.Collections.Generic;

// OneDrive for reference
//0: play state
//1: victory state
//2: preGame state
//3: chargin state

public class GameStateManager : MonoBehaviour {
	public int gameStateId = 0;
	public GameObject level;
	public GameObject[] levels;
	private GameObject player1;
	private GameObject player2;
	private UniversalHealth player1Health;
	private UniversalHealth player2Health;
	private GenerateLevel levelGenerator;
	private SoundManager soundManager;
	private Events events;
	private List<Vector3> levelObjects  = new List<Vector3>();

	// Use this for initialization
	void Start () {
		player1 = GameObject.Find ("player1");
		player2 = GameObject.Find ("player2");
		player1Health = player1.GetComponent<UniversalHealth> ();
		player2Health = player2.GetComponent<UniversalHealth> ();
		//levelGenerator = GameObject.Find ("Container").GetComponent<GenerateLevel> ();
		events = GameObject.Find ("Container").GetComponent<Events>();
		setRandomLevel ();
		foreach (GameObject lvl in levels) {
			lvl.SetActive (false);
		}
		level.SetActive (true);
		saveLevel ();
		setGameStates ();
	}
	
	// Update is called once per frame
	void Update () {
		if (checkGameStateChange()) {
			print ("Game State Change");
			toNextState();
		}
	}

	private void saveLevel(){
		levelObjects.Clear ();
		Transform[] elements = level.GetComponentsInChildren<Transform>();
		for(int i = 0; i < elements.Length; i++)
		{
			print (elements[i]);
			print (levelObjects);
			levelObjects.Add (elements[i].position);
		}
	}

	public void setGameStates() {
		player1.GetComponent<PlayerControl>().gameStateId = gameStateId;
		player2.GetComponent<PlayerControl>().gameStateId = gameStateId;
		//levelGenerator.gameStateId = gameStateId;
		//soundManager.gameStateId = gameStateId;
		events.gameStateId = gameStateId;
	}

	private void restartLevel()
	{
		Transform[] elements = level.GetComponentsInChildren<Transform>();
		for(int i = 0; i < elements.Length; i++)
		{
			elements[i].position = levelObjects [i];
		}
		level.SetActive(false);
		setRandomLevel ();
		level.SetActive (true);
		saveLevel ();
	}

	private void setRandomLevel () {
		System.Random random = new System.Random();
		int randomNumber = random.Next(0, levels.Length);
		level = levels[randomNumber];
	}

	public bool checkGameStateChange() {
		if (gameStateId == 0) {
			if (player1Health.currentHealth <= 0 || 
				player2Health.currentHealth <= 0 ) {
				//change gameState
				return true;
			}
			if (Input.GetButtonDown ("Restart")) {
				return true;
			}
		}
		if (gameStateId == 1) {
			if (Input.GetButtonDown ("Restart")) {
				return true;
			}
		}
		if (gameStateId == 2) {
			if (Input.GetButtonDown ("Restart")) {
				return true;
			}
		}
	
		return false;
	}

	private void addVictoryScreen() {
		TextMesh victoryMessage = GameObject.Find ("Victory").GetComponent<TextMesh> ();
		if (player1Health.currentHealth <= 0) {
			if (player2Health.currentHealth <= 0) {
				victoryMessage.text = ("   2 way tie");
			} else {
				victoryMessage.text = ("    Player 2 wins");
			}
		} else {
			if (player2Health.currentHealth <= 0) {
				victoryMessage.text = ("    Player 1 wins");
			} else {
				victoryMessage.text = ("    Press enter\n      to restart");
			} 
		}
			
	}

	private void removeText() {
		GameObject.Find ("Victory").GetComponent<TextMesh> ().text = "";
	}

	private void addPreGameScreen() {
		GameObject.Find ("Victory").GetComponent<TextMesh> ().text = "     Get Ready";
	}


	private void toNextState() {
		gameStateId = (gameStateId + 1) % 3;
		setGameStates();
		print ("new game state is " + gameStateId);
		if (gameStateId == 0)
		{
			removeText ();
			//do the transition
			//restore Brightness
		}
		if (gameStateId == 1) {
			// add victory screen
			addVictoryScreen();
		}
		if (gameStateId == 2) {
			removeText ();
			addPreGameScreen ();
            SpheroManager.RestartGame();
            SpheroManager.SendStateToControllers();
			//levelGenerator.restart ();
			restartLevel();
		}
	}
}