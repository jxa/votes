var gapi = {
  hangout: {
    data: {
      onStateChanged: {
        add: function(){}
      },
      getState: function(){},
      getKeys: function(){},
      setValue: function(){},
      submitDelta: function(){}
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
}
