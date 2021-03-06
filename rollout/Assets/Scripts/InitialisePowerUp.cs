﻿using UnityEngine;
using System.Linq;
using System;
using System.Collections.Generic;

public struct PowerUp {
    public int id;
    public string name;
    public string description;
    public int value;
    public bool instant;
}

public class InitialisePowerUp: MonoBehaviour
{
    public List<PowerUp> powerUps = new List<PowerUp>();

    private int id;

    void Start()
    {
        PowerUp powerUp;

        powerUp.id = newID();

        powerUp.name = "Boost";
        powerUp.description = "Gives you a speed boost";
        powerUp.value = 1;
        powerUp.instant = true;
        powerUps.Add(powerUp);

        powerUp.id = newID ();
        powerUp.name = "Damage Enemy";
        powerUp.description = "Deals damage to enemy player";
        powerUp.value = 20;
        powerUp.instant = false;
        powerUps.Add(powerUp);

        powerUp.id = newID ();
        powerUp.name = "Stun Enemy";
        powerUp.description = "Freezes enemy";
        powerUp.value = 3;
        powerUp.instant = false;
        powerUps.Add(powerUp);

        powerUp.id = newID ();
        powerUp.name = "Slow Down Enemy";
        powerUp.description = "Slows down enemy player";
        powerUp.value = 2;
        powerUp.instant = false;
        powerUps.Add(powerUp);

        powerUp.id = newID ();
        powerUp.name = "Regeneration";
        powerUp.description = "Player regenerates health continuously";
        powerUp.value = 1;
        powerUp.instant = false;
        powerUps.Add(powerUp);

        powerUp.id = newID();
        powerUp.name = "Shield";
        powerUp.description = "Gives you a shield which deflects weapons";
        powerUp.value = 50;
        powerUp.instant = true;
        powerUps.Add(powerUp);


        foreach (PowerUp p in powerUps)
            Debug.LogFormat("POWERUP: {0} -> {1}.", p.id, p.name);

    }

    int newID()
    {
        // if (id == null) {
        //     id = 0;
        // } else {
        //     id++;
        // }
        // return id;

        return id++;
    }
}
