using UnityEngine;

public class DamageSlowRegion : Region
{
    public int Damage;
    public float DragMultiplier;
	private GameObject music;

    public override void ApplyEffect(PlayerControl player)
    {
        if (Time.time > lastEffectCall + Rate)
        {
            player.GetComponent<UniversalHealth>().damagePlayer(Damage);
            lastEffectCall = Time.time;
			Debug.LogFormat ("damage here");
			music = player.gameObject.transform.Find("sound").gameObject;
			SoundManager manager = (SoundManager) music.GetComponent(typeof(SoundManager));
			manager.Stun ();
        }
    }

    public override void OnPlayerEnter(PlayerControl player)
    {
        Debug.LogFormat("PLAYER {0} ENTER REGION", player.name);

        #if SOFTWARE_MODE
        player.GetComponent<Rigidbody>().drag *= DragMultiplier;
        #endif
    }

    public override void OnPlayerLeave(PlayerControl player)
    {
        Debug.LogFormat("PLAYER {0} LEFT REGION", player.name);
        #if SOFTWARE_MODE
        player.GetComponent<Rigidbody>().drag /= DragMultiplier;
        #endif
    }

}