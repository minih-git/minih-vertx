<template>
  <div id="bg">
    <div id="error-model">
      <h1>抱歉，您无权访问该页面</h1>
      <a style="margin-top: 100px;cursor: pointer" href="/home">返回首页</a>
    </div>
  </div>
</template>

<script lang="ts" setup>

</script>

<style scoped>
#bg {
  --model-with: 800px;
  --login-model-padding: 60px;
  --background-color: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
}

#bg {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: var(--background-color);

  #error-model {
    width: var(--model-with);
    height: calc(var(--model-with) / 2);
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 15px;
    overflow: hidden;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    color: #FFF
  }
}


</style>
