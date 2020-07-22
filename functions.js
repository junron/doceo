const functions = require('firebase-functions');
const admin = require('firebase-admin');


admin.initializeApp();
const fieldValue = admin.firestore.FieldValue;

exports.createAssignment = functions.https.onCall((data, context) => {
  if (!context.auth) return {success: false, message: "Please sign in first."};
  if (!data.name) return {success: false, message: "Please provide a name."};
  if (!(typeof data.name === 'string' || data.name instanceof String)) return {success: false, message: "name must be a string."};

  var code;
  return admin.firestore().doc("lookup/assignment-name").get()
  .then(documentReference => {
    while (true) {
      code = gen9Code();
      if (!documentReference.get(code)) break;
    }

    admin.firestore().collection("assignments").doc(code).set({
      name: data.name || "Assignment",
      code: code,
      owner: context.auth.uid,
      submissions: []
    });

    let lookupData = {};
    lookupData[code] = data.name || "Assignment";
    admin.firestore().doc("lookup/assignment-name").set(lookupData, {merge: true});

    return admin.firestore().collection("users").doc(context.auth.uid).get();
  })
  .then(ref => {
    newSub = ref.get("assignments");
    newSub.push(code)
    return admin.firestore().collection("users").doc(context.auth.uid).set({
      assignments: newSub
    }, {merge: true});
  })
  .then(ref => {
    return Promise.resolve(JSON.stringify({success: true, message: code}));
  });
});

exports.deleteAssignment = functions.https.onCall((data, context) => {
  if (!context.auth) return {success: false, message: "Please sign in first."};
  if (!data.code) return JSON.stringify({success: false, message: "Please provide a code."});
  if (!(typeof data.code === 'string' || data.code instanceof String)) return JSON.stringify({success: false, message: "code must be a string."});

  var submissions;
  var ref1;
  return admin.firestore().collection("assignments").doc(data.code).get()
  .then((ref) => {
    if (ref.get("owner") != context.auth.uid) throw new Error(data.code + " does not belong to you.");
    ref1 = ref;
    return Promise.all([ref.get("submissions"), admin.firestore().collection("users").doc(context.auth.uid).get()]);
  })
  .then((rets) => {
    submissions = rets[0];
    let promises = [];

    let lookupUpdate = {};
    lookupUpdate[data.code] = fieldValue.delete();
    admin.firestore().collection("lookup").doc("assignment-name").update(lookupUpdate);
    for (let submissionID of submissions) {
      promises.push(admin.firestore().collection("submissions").doc(submissionID).update({code: "DELETED"}));
    }

    let asses = rets[1].get("assignments");
    let index = asses.indexOf(data.code);
    if (index > -1) {
      asses.splice(index, 1);
      rets[1].ref.update({assignments: asses});
    }

    ref1.ref.delete();
    return Promise.resolve(JSON.stringify({success: true, message: "Success!"}));
  }).catch(e => {
    console.log(e);
    return Promise.resolve(JSON.stringify({success: false, message: e.message}));
  });;
});

exports.getSubmissionsByAssignment = functions.https.onCall((data, context) => {
  if (!context.auth) return JSON.stringify({success: false, message: "Please sign in first."});
  if (!data.code) return JSON.stringify({success: false, message: "Please provide a code."});
  if (!(typeof data.code === 'string' || data.code instanceof String)) return JSON.stringify({success: false, message: "code must be a string."});
  var assignmentRef;
  var uidEmail;
  var uidName;
  return Promise.all([
    admin.firestore().collection("assignments").doc(data.code).get(),
    admin.firestore().doc("lookup/uid-email").get(),
    admin.firestore().doc("lookup/uid-name").get()
  ])
  .then(vals => {
    uidEmail = vals[1];
    uidName = vals[2];
    return vals[0];
  })
  .then(ref => {
    if (ref.get("owner") != context.auth.uid) throw new Error(data.code + " does not belong to you.");

    let promises = [];
    for (let submissionID of ref.get("submissions")) {
      promises.push(admin.firestore().collection("submissions").doc(submissionID).get());
    }
    return Promise.all(promises);
  })
  .then(vals => {
    let submissions = {};
    for (let ref of vals) {
      submissions[ref.id] = {
        images: ref.get("images"),
        email: uidEmail.get(ref.get("owner")),
        name: uidName.get(ref.get("owner")),
        time: ref.get("time"),
        comment: ref.get("comment")
      };
    }
    return Promise.resolve(JSON.stringify({success: true, message: submissions}));
  })
  .catch(e => {
    console.log(e);
    return Promise.resolve(JSON.stringify({success: false, message: e.message}));
  });
});

exports.getSubmissionsByUser = functions.https.onCall((data, context) => {
  if (!context.auth) return JSON.stringify({success: false, message: "Please sign in first."});
  var assignmentName;
  return Promise.all([admin.firestore().collection("users").doc(context.auth.uid).get(), admin.firestore().doc("lookup/assignment-name").get()])
  .then(vals => {
    assignmentName = vals[1];
    return vals[0];
  })
  .then(ref => {
    let submissions = ref.get("submissions");
    let promises = [];
    for (let submission of submissions) {
      promises.push(admin.firestore().collection("submissions").doc(submission).get());
    }
    return Promise.all(promises);
  })
  .then(vals => {
    console.log(vals);
    let submissions = {};
    for (let ref of vals) {
      submissions[ref.id] = {
        images: ref.get("images"),
        time: ref.get("time"),
        name: assignmentName.get(ref.get("code")) || ref.get("code"),
        code: ref.get("code"),
        comment: ref.get("comment")
      };
    }
    return Promise.resolve(JSON.stringify({success: true, message: submissions}));
  })
  .catch(e => {
    console.log(e);
    return Promise.resolve(JSON.stringify({success: false, message: e.message}));
  });
});

exports.createSubmission = functions.https.onCall((data, context) => {
  if (!context.auth) return JSON.stringify({success: false, message: "Please sign in first."});
  if (!data.images) return JSON.stringify({success: false, message: "Please provide images."});
  if (!Array.isArray(data.images)) return JSON.stringify({success: false, message: "images must be an array."});
  if (!data.code) return JSON.stringify({success: false, message: "Please provide a code."});
  if (!(typeof data.code === 'string' || data.code instanceof String)) return JSON.stringify({success: false, message: "code must be a string."});
  data.code = data.code.toUpperCase();
  data.code = data.code.replace("0", "O");

  var ref1;
  var ref2;
  return admin.firestore().collection("assignments").doc(data.code).get()
  .then(ref => {
    ref1 = ref;
    if (!ref.exists) throw new Error(data.code + " does not exist");
    return admin.firestore().collection("submissions").add({
      images: data.images,
      code: data.code,
      owner: context.auth.uid,
      time: admin.firestore.FieldValue.serverTimestamp(),
      comment: ""
    });
  }).then(ref => {
    ref2 = ref;
    let arr = ref1.get("submissions");
    arr.push(ref.id);
    ref1.ref.set({submissions: arr}, {merge: true});

    return admin.firestore().collection("users").doc(context.auth.uid).get();
  })
  .then(ref => {
    let arr = ref.get("submissions");
    arr.push(ref2.id);
    ref.ref.set({submissions: arr}, {merge: true});

    return Promise.resolve(JSON.stringify({success: true}));
  })
  .catch(e => {
    console.log(e);
    return Promise.resolve(JSON.stringify({success: false, message: e.message}));
  });
});

exports.writeComment = functions.https.onCall((data, context) => {
  if (!context.auth) return JSON.stringify({success: false, message: "Please sign in first."});
  if (!data.code) return JSON.stringify({success: false, message: "Please provide a code."});
  if (!(typeof data.code === 'string' || data.code instanceof String)) return JSON.stringify({success: false, message: "code must be a string."});
  if (!data.id) return JSON.stringify({success: false, message: "Please provide a id."});
  if (!(typeof data.id === 'string' || data.id instanceof String)) return JSON.stringify({success: false, message: "id must be a string."});
  if (!data.comment) return JSON.stringify({success: false, message: "Please provide a comment."});
  if (!(typeof data.comment === 'string' || data.comment instanceof String)) return JSON.stringify({success: false, message: "comment must be a string."});


  var submissionRef;
  var assignmentRef;
  return Promise.all([admin.firestore().collection("submissions").doc(data.id).get(), admin.firestore().collection("assignments").doc(data.code).get()])
  .then(vals => {
    submissionRef = vals[0];
    assignmentRef = vals[1];

    console.log(assignmentRef.get("owner")+"   "+context.auth.uid);
    if (assignmentRef.get("owner") != context.auth.uid) throw new Error(data.code+" does not belong to you");
    if (submissionRef.get("code") != data.code) throw new Error(data.id+" does not belong to "+data.code);

    return submissionRef.ref.update({comment: data.comment});
  })
  .then(ref => {
    return Promise.resolve(JSON.stringify({success: true, message: "success"}));
  })
  .catch(e => {
    console.log(e);
    return Promise.resolve(JSON.stringify({success: false, message: e.message}));
  });;

});

exports.onUserCreate = functions.auth.user().onCreate((user) => {
  var uidEmail = {};
  uidEmail[user.uid] = user.email;
  admin.firestore().doc("lookup/uid-email").set(uidEmail, {merge: true});
  var emailUid = {};
  emailUid[user.email] = user.uid;
  admin.firestore().doc("lookup/email-uid").set(emailUid, {merge: true});
  var uidName = {};
  uidName[user.uid] = user.displayName || "";
  admin.firestore().doc("lookup/uid-name").set(uidName, {merge: true});
  admin.firestore().collection("users").doc(user.uid).set({
    assignments: [],
    submissions: []
  })

  return {success: true};
});

function gen9Code() {
  var result = '';
   var characters  = '123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
   var charactersLength = characters.length;
   for ( var i = 0; i < 9; i++ ) {
      result += characters.charAt(Math.floor(Math.random() * charactersLength));
   }
   return result;
}

function nullTypeSafe(value, type, fallback){
  if (value !== null && typeof value == type) return value;
  return fallback;
}

function nullTypeSafeArray(value) {
  if (value !== null && Array.isArray(value)) return value;
  return [];
}
