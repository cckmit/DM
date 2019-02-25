
function synthesisReply(moban) {
 return moban.replace(/\$[^$]+\$/g, function(matchs) {
  matchs = matchs.replace(/\$/g, "");
  var returns = eval(matchs);
  return (returns + "") == "undefined"? "": returns;
 });
}

function isMonthRange(month) {
 var tempt = month.replace(/[^0-9]/ig,"");
 if(!/^\d{6}$/.test(tempt))
  return false
 tempt = month.substr(0,4)+'/'+tempt.substr(4,6)
 input = new Date(tempt)

 if(input>new Date())
  return false

 input.setMonth(input.getMonth()+13)
 return input>new Date()

}


function numberSame(new_password,new_password_confirm){
 if(new_password == new_password_confirm)
  return true;
 else
  return false;

}

//function checkPassword(idCard,tel,new_password){
//
// if (/^(\d)\1+$/.checkPassword(new_password)) return false;  // 全一样
//
// var str = new_password.replace(/\d/g, function($0, pos) {
//  return parseInt($0)-pos;
// });
// if (/^(\d)\1+$/.checkPassword(str)) return false;  // 顺增
//
// str = new_password.replace(/\d/g, function($0, pos) {
//  return parseInt($0)+pos;
// });
// if (/^(\d)\1+$/.checkPassword(str)) return false;  // 顺减
// return true;
//}


function checkPassword(idCard,tel,new_password){
 var a1 = new_password.substring(0,1);
 var a2 = new_password.substring(1,2);
 var a3 = new_password.substring(2,3);
 var a4 = new_password.substring(3,4);
 var a5 = new_password.substring(4,5);
 var a6 = new_password.substring(5,6);

 if(a1 == a2 && a2 == a3 && a3 == a4 && a4 == a5 && a5 == a6 )
   return false;
 if(parseInt(a1)-parseInt(a2) == 1 && parseInt(a2)-parseInt(a3) == 1
     && parseInt(a3)-parseInt(a4) == 1 && parseInt(a4)-parseInt(a5) ==1 && parseInt(a5)-parseInt(a6) == 1)
   return false;
 if(parseInt(a1)-parseInt(a2) == -1 && parseInt(a2)-parseInt(a3) == -1
     && parseInt(a3)-parseInt(a4) == -1 && parseInt(a4)-parseInt(a5) ==-1 && parseInt(a5)-parseInt(a6) == -1)
   return false;
 if(idCard.indexOf(new_password) != -1)
   return false;
 if(tel.indexOf(new_password) != -1)
   return false;
 return true;
}

