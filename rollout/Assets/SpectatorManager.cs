﻿using System;
using System.Collections.Generic;
using System.Net;
using System.Threading;

using UnityEngine;

public class VoteEventArgs : EventArgs
{
    public int Id       { get; private set; }
    public int Votes    { get; private set; }

    public VoteEventArgs(int id, int votes)
    {
        Id = id;
        Votes = votes;
    }
}

public static class SpectatorManager
{
    public static EventHandler<VoteEventArgs> VoteWinnerDetermined;

    public static List<Spectator> Instances { get; private set; }

    public static Events   EventManager { get; private set; }

    private static int[]    eventVoteCounts;
    private static System.Object   lockSync;

    static SpectatorManager()
    {
        Instances = new List<Spectator>();
        // Hardcoded size, needs to be adjusted for when more are added. Should also
        // probably be loaded from a config file.
        eventVoteCounts = new int[3];

        lockSync = new System.Object();
    }

    public static void Initialise()
    {
        EventManager = GameObject.Find("Container").GetComponent<Events>();
    }

    public static void SendToAll(ServerMessage message)
    {
        byte[] data = message.Compile();
        foreach (Spectator s in Instances)
            Server.Send(data, s.Target);
    }

    public static void SendNewEvents()
    {
        // Hardcode events for now.
        byte[] eventIds = new byte[] { 1, 2 };

        // Set up message.
        ServerMessage message = new ServerMessage(ServerMessageType.SetEvents);
        foreach (byte id in eventIds)
            message.AddContent(id);
        int time = 10000;
        message.AddContent(time); // Countdown time in ms.

        for (int i = 0; i < eventVoteCounts.Length; ++i)
            eventVoteCounts[i] = 0;

        SendToAll(message);

        new Thread(() =>
        {
            Thread.CurrentThread.IsBackground = true;
            Thread.Sleep(time);
            VoteEventArgs winner = GetEventVoteWinner();
            if (VoteWinnerDetermined != null)
                VoteWinnerDetermined(null, winner);
        }).Start();
    }

    public static void EventVote(int eventId)
    {
        lock (lockSync)
        {
            ++eventVoteCounts[eventId];
        }
    }

    private static VoteEventArgs GetEventVoteWinner()
    {
        int max = -1, id = -1;
        for (int i = 0; i < eventVoteCounts.Length; ++i)
        {
            if (eventVoteCounts[i] > max)
            {
                max = eventVoteCounts[i];
                id = i;
            }
        }

        return new VoteEventArgs(id, max);
    }
}

public class Spectator
{
    public IPEndPoint Target { get; set; }

    public Spectator(IPEndPoint target)
    {
        Target = target;
    }
}
