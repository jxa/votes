var gapi = {
  hangout: {
    data: {
      onStateChanged: {
        add: function(){}
      },
      onMessageReceived: {
        add: function(){}
      },
      getState: function(){},
      getKeys: function(){},
      setValue: function(){},
      submitDelta: function(){},
      sendMessage: function(){}
    },
    layout: {
      displayNotice: function(){}
    },
    onApiReady: {
      add: function(){}
    },
    onEnabledParticipantsChanged: {
      add: function(){}
    },
    getLocalParticipant: function(){},
    getEnabledParticipants: function(){}
  }
};

gapi.hangout.Participant = {
  person: {
    id: null,
    displayName: null,
    image: {url: null}
  }
};

gapi.hangout.EnabledParticipantsChangedEvent = {
  enabledParticipants: null
};

gapi.hangout.EnabledParticipantsChangedEvent = {
  enabledParticipants: null
};

gapi.hangout.data.StateChangedEvent = {
  state: null
};

gapi.hangout.data.MessageReceivedEvent = {
  senderId: null,
  message: null
};
