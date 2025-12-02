# Furhat Lab

## Preparations before the lab

We strongly advise you to use a headset instead of your laptop's internal microphone and speakers. We also recommend you to install everything and make sure it runs before you come to the lab, so that you can use the time at the lab to learn as much as possible. 

### Installation and setup

1. You need to install two pieces of software before you can do the lab.

   * [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/). Links to an external site. This is the development environment (IDE) supported by the Furhat SDK. (Please note that the download page links to both Ultimate Edition and Community Edition; do not download Ultimate Edition, it requires a license.)
   * [Furhat SDK](https://furhat.io/): You need to sign up ("Get SDK") to be able to download and install the Furhat SDK. Follow [these instructions](https://docs.furhat.io/launcher/) for more details. 

2. Use Git to clone this repo into a folder on your computer.

3. Open the folder in IntelliJ. Follow [these instructions](https://docs.furhat.io/skills/#importing-the-skill-into-intellij-ide) if you encounter problems.

### Using the Furhat SDK web interface

If you have started the SDK Launcher and your virtual Furhat is running, you should have a window open on your computer with a 3D view of the Furhat's face. You can click "open web interface" in the Launcher (or visit "localhost:8080" in a web browser) to open up the Furhat web administration interface.

The Furhat web admin interface needs a password to log in, which is "admin" by default. On the left side of the web interface, there are tabs labelled "home", "dashboard", "wizard" and "settings". Under "home", you can make sure that your headphones can play audio correctly by making the speech synthesis say something (the green "play" button). You should also test that the microphone is working by pressing the "listen" button and speaking into the microphone. Keep in mind that the Furhat SDK only accepts microphones and speakers that were plugged in before you started the virtual Furhat, so if you plug in your headphones after the virtual Furhat head pops up, it won’t work until you restart the Furhat SDK.

When you have gotten this to work, the "dashboard" tab is where you want to be for most of the remainder of the lab. You can double-click in the area around the Furhat robot in the middle of the page to add virtual, simulated, users (since your simulated Furhat does not have a camera). These simulated users can be moved around by dragging them, and deleted by double-clicking them. There is an inner and outer interaction zone. When the user enters the inner zone, a `UserEnter` event is raised, and when the user leaves the outer zone, a `UserLeave` event is raised. This way, your skill will be notified when an interaction should start and end. 

You can now run the skill from IntelliJ. If you open the folder `src/main/kotlin/furhatos.app.furhatlab`, you should see a file named `main.kt` which you can open. This is the starting point of the skill. Next to the `main()` function, there should be a green arrow. Clink on it to run the skill. Then go back to the dashboard and make sure there is a virtual user within the interaction distance to Furhat. Furhat should then start the interaction with you.  

### Navigating the code

In the `main.kt` file, you will see that the interaction flow will start with the state `Init`.
You can follow references in IntelliJ by pressing the Control/Command key and clicking on them. Do this to open the `Init`state.
Here, basic parameters for the skill are configured.
After that, you will see that the flow checks whether any user is present or not, which will make the flow go to either `Idle` or `StartInteraction`. 

In `StartInteraction`, you will see that the flow continues to either `FruitSellerGreeting` (Part 1 of the lab) or the `ChatState` (Part 2 of the lab). Here, you can select which one you want to work with. You should spend around two hours of the lab on Part 1 and one hour on Part 2. If you are not done with all the tasks in Part 1 after about two hours, you can skip the rest of Part 1 and move on to Part 2.

You can read more about the SDK and how do program interactions in the [Furhat docs](https://docs.furhat.io/). We will only mention some of the functionality here, but you can find much more about how to implement your ideas in the docs.  

## Part 1: Fruit seller

In this part of the lab, you will experiment with a very simple state- and intent-based approach to modelling dialogue. The relevant file for this part of the lab is `flow/FruitSeller.kt`.

Start by reading the code to understand it. The flow is structured through a set of states and transitions (`goto`) between them. Note that there is a state hierarchy, where for example the `TakingOrder` state inherits the `Interaction` state. The triggers in the parent state are also active in the child state, and can be overriden by the child (much like class inheritance in object-oriented programming).  

Try out the interaction a few times to explore the functionality. You can easily end and restart the interaction by removing and adding a new virtual user in the dashboard. If you change the code, you have to restart the skill for the changes to take effect. After that, you are now ready for some exercises!

### Asking the user to confirm the order

At the end of the interaction, after reading back the order, define a new `Confirmation` state where Furhat asks the user if this is what they want to order (a yes/no question). If yes, end the interaction. If no, you could restart the order.

### Adding gestures and varied utterances

* Add suitable facial gestures for Furhat with `furhat.gesture()`, which you can [read about here](https://docs.furhat.io/gestures/#performing-gestures).

* Through the [Utterance functionality](https://docs.furhat.io/speech/#utterances), you can insert gestures in the middle of utterances, and randomize what Furhat says to make it more varied. You

### Handle other user behaviours 

The flow is currently very rigid and there are not a lot of potential intents that are handled. Try to add handlers for things you think a user might say, such as "What fruits do you have?" 

### Handling a second user

If a second customer appears during the interaction, Furhat should turn to that customer, tell the new customer to wait until the current order is complete, and then return to the current customer. When the current customer leaves, Furhat should turn to the new customer to take the order.

Your `Interaction` state (which is a parent state for most of the interaction) should already have a trigger that looks like this:

```
onUserEnter(instant = true) {
    furhat.glance(it)
}
```

This trigger will react to any user that enters the interaction space while the system is trying to sell fruit to someone else, and glance at the new user. The behaviour we want instead is:

1. Attend the new user (`furhat.attend(it)`).
2. Say "I will help you later", or something similar.
3. Attend the original user (`furhat.attend(users.other)`) and resume the interaction (you can do a `reentry()` restart the current state). 

Note: The `onUserEnter` is currently marked `instant = true`, since the glancing is supposed to take place simultaneously with other behaviours. You should remove this flag from the trigger, since you want Furhat to abort what it was currently doing for this new behaviour.  
 
### Other things

If you have more than 1 hour left of the lab, try these challenges, before moving on to Part 2:

* Ask for a delivery time. You can use the built-in NLU entity `Time`
* As the user for their name and refer to the user by their name. You can use the built-in NLU entity `PersonName`
* Add prices to the fruits. Each fruit has an associated price. The user should be able to ask for the individual prices of fruits. When the robots sums up the order, it should also state the total price.
* Or other ideas you might come up with!

## Part 2: Chatbot

In this part of the lab, you will explore the use of LLMs for conversational systems. 
In the `StartInteraction` state, switch to the `ChatState`. Open `ChatState.kt` and insert the OpenAI API key, as provided on Canvas. Try to run the skill and interact with it.  

### Changing the system prompt

Change the `systemPrompt` in the `ResponseGenerator` constructor and try out different characters, such as a Virtual Patient. In the dashboard, you can try to change the voice and face ("character") of the robot to fit your character.   

### Using the Chatbot as a fallback in FruitSeller

Try to add the `ChatState` as a parent to the `Interaction` state in `FruitSeller` and remove the generic `onResponse` handler in the `Interaction` state. When you say something that the fruit seller cannot handle, the `ChatState` should take care of it.
