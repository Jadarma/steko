# Stego Security

This documents comments on the security level of the Stego algorithm.
In essence, it is just an extension over [`AES-GCM-256`](https://en.wikipedia.org/wiki/Galois/Counter_Mode), using the
same key to deterministically scramble the ciphertext inside a carrier's image data.

The safety of the steganographic secrets depend entirely on the op-sec of managing the keys.

## 🔦 Detection

**You cannot steal what you cannot find.**

Extracting the payload is difficult, but so is determining if there is one at all.
Depending on the payload to carrier size ratio, differences in images are imperceivable with the human eye, and due to
the random shuffling, difficult to detect by machines too.

Compared to simply encrypting the file as-is, this also protects you from the [$5 wrench](https://xkcd.com/538/), if
adversaries cannot determine even the existence of said secret.

## 👺 Adversarial Challenge

Supposing an adversary is convinced an image is a carrier for a steganographic payload, to prove it and extract the
secret, two conditions must be met:

1. The correct pixel order and length of the payload must be known.
2. The AES ciphertext must be decrypted.

## 🧩 Unshuffling

The possible orderings of the pixels in a 12MP image is:

```
(3000 x 4000)! = 12.000.000! > 2^256 
```

That value is larger than the `2^256` key, and since the key is also required for decryption, the only reasonable
approach for unshuffling is to brute-force the key.

It should be noted that the PRNG used is not cryptographically secure, and in theory may be reversed if enough of the
order is known. However, the payload at rest is entirely gibberish:

- The header challenge is XOR-ing of random bits
- The length is just a number meaningless without context
- The payload is random-looking AES ciphertext

On top of that, supposing the order is known, it is difficult to determine in which state the PRNG is because it
generates 64-bit numbers, whereas the shuffle only uses half of them and discards the rest.

Finally, even if somehow the seed is determined, that is the hash of the actual key, so if random bits or very strong
passphrases are used, obtaining them is impossible.

## 🔑 Key Entropy

The key is a 256-bit number, but the leading 32 bits are used to encode the bitmask, which in practical terms is reduced
to a very small subset of possible values.

Leaving those out, the key still has 224 bits of entropy.
Unless a backdoor is discovered in AES, brute-forcing is on a heat-death of the universe scale of difficulty.

While sacrificing from the scale of true AES-256, the benefit of obfuscation makes up for it.
It is still better than AES-192, and even AES-128 is still considered secure.
