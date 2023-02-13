# NeonOut

***NeonOut*** is program that enables NeonMob users to download and keep theirs collections after NeonMob gets out of business, which is planned to take place at 2023-02-28.

You can download pre-built executable scripts (*NeonOut.cmd* for **Windows**; *NeonOut.sh* for **macOS** and **Linux**) at the [releases page](https://github.com/vipseixas/NeonOut/releases/).

## Prerequisites

You'll need Java 11+ installed in your system to run these executables, I recommend using installers from [Adoptium](https://adoptium.net/temurin/releases/).

## Running

After downloading the appropriate script and executing it, a terminal window will open with a prompt to your NeomMob username. 
Just type your username, press enter and the program must start downloading your collections. 
The destination path will be logged to the screen.

For Linux and macOS users: you may have to give execution permission to the file before executing. 

## More details

* No login/password is required, the NeonMob API is public
* All images/videos are downloaded regardless of the user having it in their collection
* The program stores metadata about the collections and its cards (e.g.: rarity, cards owned, # of prints, etc), so in the future it will be possible to recreate a browsable page showing all the current information (that's in my personal roadmap)
* You can run the program multiple times, and it will only download not previously downloaded information, including collections' metadata
* If you want to update some collection metadata (e.g.: the number of cards you own), you'll have to erase the metadata file from your computer
* To avoid being blocked by NM servers, there is a 5-second pause for each 10 API requests made
* Images/videos are downloaded without the above-mentioned pause for they are downloaded from a CDN
* The program error handling is very basic due the time constraints to get it ready, if something strange happens, just kill it and run it again
