# BirthdayMan - a CLI App for managing birthday reminders

## Introduction

This is a CLI app based on the app created by Dan Amber as part of an online video / tutorial about
[NBB](https://github.com/babashka/nbb) - here's a link to his [project](https://github.com/danownsthisspace/birthman)

In the spirit of trying to learn NBB for myself but get a flying start from Dan's excellent video, I decided
to change the DB to sqlite3 and to add another bit of data (what the person wants for their birthday), and
then add extra functionality to change the gift idea and to be able to search for birthdays on an arbitrary date.

[Here's a link to Dan's video](https://youtu.be/_-G9EKaAyuI) - The video is Dan coding and getting pair assistance
from the excellent [borkdude](https://github.com/borkdude), creator of Babashka and NBB amongst other awesome stuff
from the Clojure Ecosystem.

## Usage - Installation

npm install -g birthdayman-sqlite

To use the app simply use the following commands:

* birthdayman
* birthdayman help
* birthdayman list
* birthdayman list-people
etc.

You may find it useful to append a call to birthdayman using the "reminder" argument, thus:

birthdayman reminder [month]

to your shell config, so that you get a list of upcoming birthdays for the current and following month when launching a new terminal.

## Usage - Development

If you just want to play around with this for yourself the simplest way to do that is to clone the repo, and then follow these steps:

1. npm install -g nbb
2. npm install

(You will need a current version of NodeJS to satisfy the dependency needs of NBB)

## Usage - Running the app in development

Simply run the following in the root of the project to add a Birthday that you want to remember:

node index.mjs

If you want to see a list of commands available, use:

node index.mjs help