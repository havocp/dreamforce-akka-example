# Akka + Heroku Example, Dreamforce 2012

This is a simple Scala/SBT/Akka skeleton app that runs on Heroku.

The idea is to have a minimum of code and just show you how to set
up an SBT build for Heroku and spawn Akka actors.

HTTP is handled with Netty, but in real life you probably want to
use something higher-level such as Play Framework.

## Setup

These need to be installed manually:

- a working `heroku` command on your path;
  [here are the instructions][herokucommand]
- Simple Build Tool (SBT) 0.11 setup on your path;
  [here are the instructions][xsbtsetup]

## How to run it on Heroku

In brief:

 - Install the Heroku tools; be sure `heroku` is on your path
    - see http://devcenter.heroku.com/articles/heroku-command
 - Type these commands inside the application's git clone:
    - `heroku create`
    - `git push heroku master`
    - `heroku open`

Here's what each step does.

`heroku create` creates a new Heroku application.

    $ heroku create --stack cedar
    Creating hollow-night-3476... done, stack is cedar
    http://hollow-night-3476.herokuapp.com/ | git@heroku.com:hollow-night-3476.git
    Git remote heroku added

The application name `hollow-night-3476` is randomized and will vary.

Now, you're ready to push the application to Heroku. This can take a couple
minutes, it's slower the first time since it has to download the
Internet. Type `git push heroku master` and you should see:

    $ git push heroku master
    Counting objects: 1220, done.
    Delta compression using up to 4 threads.
    Compressing objects: 100% (636/636), done.
    Writing objects: 100% (1220/1220), 164.40 KiB, done.
    Total 1220 (delta 284), reused 0 (delta 0)

    -----> Heroku receiving push
    -----> Scala app detected
    -----> Building app with sbt v0.11.0
    -----> Running: sbt clean compile stage

    ... lots of output here related to updating and compiling ...

    -----> Discovering process types
           Procfile declares types -> indexer, web
    -----> Compiled slug size is 52.4MB
    -----> Launching... done, v7
           http://hollow-night-3476.herokuapp.com deployed to Heroku

    To git@heroku.com:hollow-night-3476.git
     * [new branch]      master -> master

View the application with `heroku open` or by typing
`http://APPNAME.herokuapp.com/` into a browser.

If anything goes wrong, you can check the application's logs with `heroku
logs`; use `heroku logs --num 10000` to get 10000 lines of logs if you need
more than the default log size.

## Enjoy

Learn more about the Scala and Akka stack at http://typesafe.com/ !

[herokucommand]: http://devcenter.heroku.com/articles/heroku-command "How to install Heroku"

[xsbtsetup]: https://github.com/harrah/xsbt/wiki/Getting-Started-Setup "SBT setup"

