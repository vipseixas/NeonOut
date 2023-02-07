# NeonOut

***NeonOut*** is a program that enables NeonMob users to download and keep theirs collections after NeonMob gets out of business, which is planned to take place at 2023-02-28.

It is written in Scala 3 and can be executed using [SBT](https://www.scala-sbt.org/) or [scala-cli](https://scala-cli.virtuslab.org/).

For the non-technical people there are pre-built executable scritps at the [releases page](https://github.com/vipseixas/NeonOut/releases/).

## Prerequisites

You'll need Java 11+ installed in your system to run these executables, I recommend using installers from [Adoptium](https://adoptium.net/temurin/releases/).

## Running

The executable runs on a terminal (*cmd.exe* for Windows or *bash* for Linux/MacOS) and accepts 2 parameters, the UserID and the SettID (Sett is how a Collection is named by NM API). Both parameters are mandatory but SettID can be *0* indicating that all collections are to be downloaded.

Example:

> NeonOut.cmd 987654 0

## How do I find my NeonMob user ID?

The tricky think is that NeonMob does't show your ID anywhere, so to get it you'll have to inspect the browser cookies. There is a good tutorial on how to do this [here](https://cookie-script.com/documentation/how-to-check-cookies-on-chrome-and-firefox).

You'll have to select the cookies for *https://www.neonmob.com* and look for the value of the ***ajs_user_id*** cookie.

## More details

* The program collects metadata about the collections, so in the future it will be possible to recreate a browseable page showing all the current information (that's in my personal roadmap)
* You can run the program multiple times and it will only download not previously downloaded information, including collections' metadata
* If you want to update some collection metadata (e.g.: the number of cards you own), you'll have to erase the metadata file from your computer
* To avoid being blocked by NM servers, there is a 5 second pause for each 10 requests made
* The program error handling is very basic due the time constraints to get it ready, if something strange happens, just kill it and run it again