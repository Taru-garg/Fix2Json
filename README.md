# Fix2Json

## Overview
Fix2Json is a simple JAVA CLI application that converts a given raw [FIX](https://www.fixtrading.org/what-is-fix/) strings ( which are generally very hard to understand ) to their JSON equivalents ( which one can understand much more clearly ).

The application has been built using the following libraries:
- [picocli](https://picocli.info/)
- [simple-json](https://github.com/fangyidong/json-simple)
- [QuickFIX/J](https://www.quickfixj.org/)

## Using the application
- Clone the repository using the following command
    ```shell
    git clone https://github.com/Taru-garg/Fix2Json
    ```
- If you have IntelliJ or some other IDE which enables you to directly build JAVA projects, 
  then great! Not a lot of things to do. Just build the project and run it. You can simply pass in --help or -h as
  the command line argument.

- To build the repository from scratch follow the steps
  ```shell
  brew install mvn
  # Once in the repo run
  mvn package
  # this would create a jar in target directory which you can run as follows
  java -jar target/Fix2Json-1.0-jar-with-dependencies.jar --help
  ```

## Sample generated
The following section shows a sample message that can be generated from a given FIX message.

<details>
<summary>
Raw FIX String
</summary>

```
8=FIX.4.4|9=247|35=s|34=5|49=sender|52=20060319-09:08:20.881|56=target|22=8|40=2|44=9|48=ABC|55=ABC|60=20060319-09:08:19|548=184214|549=2|550=0|552=2|54=1|453=2|448=8|447=D|452=4|448=AAA35777|447=D|452=3|38=9|54=2|453=2|448=8|447=D|452=4|448=aaa|447=D|452=3|38=9|10=056|
```

</details>


<details>
<summary>
Generated JSON equivalent
</summary>

```json
[
  {
    "Header": {
      "BeginString": "FIX.4.4",
      "SenderCompID": "sender",
      "SendingTime": "20060319-09:08:20.881",
      "TargetCompID": "target",
      "MsgType": "s",
      "MsgSeqNum": "5",
      "BodyLength": "247"
    },
    "Body": {
      "Price": "9",
      "OrdType": "2",
      "Symbol": "ABC",
      "CrossType": "2",
      "CrossID": "184214",
      "SecurityIDSource": "8",
      "SecurityID": "ABC",
      "NoSides": [
        {
          "Side": "1",
          "OrderQty": "9",
          "NoPartyIDs": [
            {
              "PartyRole": "4",
              "PartyID": "8",
              "PartyIDSource": "D"
            },
            {
              "PartyRole": "3",
              "PartyID": "AAA35777",
              "PartyIDSource": "D"
            }
          ]
        },
        {
          "Side": "2",
          "OrderQty": "9",
          "NoPartyIDs": [
            {
              "PartyRole": "4",
              "PartyID": "8",
              "PartyIDSource": "D"
            },
            {
              "PartyRole": "3",
              "PartyID": "aaa",
              "PartyIDSource": "D"
            }
          ]
        }
      ],
      "CrossPrioritization": "0",
      "TransactTime": "20060319-09:08:19"
    },
    "Trailer": {
      "CheckSum": "056"
    }
  }
]
```

</details>

## Pending Items
- Currently, there is no validation on the message version, so we use whatever the user passes however if the messages are for some reason not the same version we will run into an error while converting them to their JSON equivalents. We need to add a validation on the message version and the version user said and skip the message if they are not the same version
