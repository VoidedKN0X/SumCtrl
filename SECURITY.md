# Security policy

Thank you for helping keep SumCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/SumCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of SumCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

SumCtrl has no server and no internet permission: everything stays on your phone. So these count:

- Any way for SumCtrl to send your data off the phone, or for your history to end up somewhere you didn't choose
- Ways around the app lock, or protected screens showing up in screenshots when they shouldn't
- Another app on the phone being able to read SumCtrl's data or make SumCtrl do something you didn't ask for
- Weaknesses in the app lock PIN
- Anything that lets someone run code or read files they shouldn't

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
A8:E6:37:6F:46:1C:99:29:FC:AF:A0:30:01:4A:A8:DE:63:36:02:36:B9:7E:55:05:DE:40:78:98:90:54:A8:09
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
