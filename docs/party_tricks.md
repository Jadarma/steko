# Party Tricks

Here are some examples of niche uses that take advantage of the customizable bitmasks.
I call them party tricks because their effectiveness relies on the other side not aware of it, or they could be
achieved via other means as well.

## 🔮 Nostradamus Images

A single carrier image may contain multiple payloads so long as the bitmasks of the keys do not overlap.
For example, you may encode different messages in the _Red_, _Green_, and _Blue_ channels respectively.
You may, of course, use more, but remember the more significant bits you use, the more distorted the image becomes.

For example, before an event, you can predict all outcomes, then share the image with your friends.

```shell 
$ steko hide -b 0x01000000 -m "I told you outcome A was gonna happen." -e image.png > outcomeA.key
$ steko hide -b 0x00010000 -m "I told you outcome B was gonna happen." -e image.png > outcomeB.key
$ steko hide -b 0x00000100 -m "I told you outcome C was gonna happen." -e image.png > outcomeC.key
```

Afterwards, you can claim you knew all along and prove it by providing the key associated to your prediction:

```shell
$ steko show image.png < secret.key # Renamed from outcomeB.key!
I told you outcome B was gonna happen.
```

## 🤝 Group Custody

Using the same logic of multiple payloads per key, one could split a single payload across multiple keys, such that all
keyholders need to collaborate in order to retrieve it.

As a narrative example, imagine you are a wise old parent preparing your will, but you do not wish to gift your estate
your three children unless they stop bickering and agree to share.
You can draft the document / deed, hide it in the same image, give each offspring their key, but instruct your lawyer to
only release the image and / or master key upon your demise.

```shell 
$ echo 'WhenImOldAndWise69' > lawyer.key
$ openssl aes-256-cbc -e -pass file:lawyer.key -pbkdf2 -in deed.pdf -out deed.enc
$ split -n 3 deed.enc 'deed-'
$ steko hide -b 0x01000000 -e image.png deed-aa > alice.key
$ steko hide -b 0x00010000 -e image.png deed-ab > bob.key
$ steko hide -b 0x00000100 -e image.png deed-ac > eve.key
```

Then, the only way they could possibly retrieve it would be to put their differences aside and share the keys in the
presence of the lawyer:

```shell
$ steko show image.png -o . < alice.key
$ steko show image.png -o . < bob.key
$ steko show image.png -o . < eve.key
$ cat deed-* | openssl aes-256-cbc -d -pass file:lawyer.key -pbkdf2 > deed.pdf
```

## 🎯 Decoy Payloads

Suppose you are a _(now compromised)_ secret agent, captured by evil villains that know you have something to hide in
your briefcase.
To avoid torture or give in to the demands to save the damsel in distress, you can prepare decoy payloads to trick your
enemies.

Use the normal, less secure, passphrase method _(so you can memorize it and confess it if needed)_ and hide a less
important, but true secret to earn trust, or maliciously feed fake information:

```shell
$ steko hide -p -e secret.png -m 'We are awaiting vital reinforcements from an east-bound convoy.'
```

However, you can hide the actual secret in the second least significant bit.
Using `--no-noise` will reduce the impact it has on image quality.

```shell
$ steko hide -b 02020200 --no-noise -e secret.png -m 'We are ready; attack at dawn.' > secret.key 
```

Hopefully the existence of the decoy payload will grant plausible deniability, trust, or buy time as you await rescue.
