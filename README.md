# clojurebot

An IRC bot with a bunch of (arguably) useful commands.

## Installation

Building from source is the only available method. I do not intend on providing
pre-compiled JAR's.

## Usage

Run the compiled JAR of the clojurebot like so:

    $ java -jar clojurebot-0.1.0-standalone.jar
    
To compile the JAR itself, use: 
    
    $ lein uberjar
    
To run using leiningen use:

    $ lein run
    
## Configuration

Configuration is provided in `resources/config.edn`.

## Available commands

`echo` - Repeats the message after echo. E.g. `echo Say something`  
`time` - Prints the clojurebot's current local time.  
`amirite?` - Confirms your correctness.  
`coin` - A heads/tails coin toss.  
`eval` - Runs (a limited subset of) the clojure evaluator on the given string.
E.g. `eval (+ 1 2)` will output `3`. (I hope...)  
`doc` - Looks up the documentation for the given symbol in clojure.core.  

## Adding Plugins

An example of how to define a plugin (currently only in `clojurebot.core`)

    (reg-command 
      "reverse"
      (fn [msg]
        (clojure.string/reverse msg))))
        
You can also just pass in the reverse function directly.

    (reg-command 
      "reverse"
      clojure.string/reverse)

### Bugs

...

## License

Copyright © 2016 Antonis Kalou

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
