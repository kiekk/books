let emotion = { name: 'sad' };
function aliasing(pEmotion) {
    pEmotion.name = 'happy';
}
console.log(emotion.name);
// emotion.name = 'happy' // 에러
aliasing(emotion);
console.log(emotion.name);
