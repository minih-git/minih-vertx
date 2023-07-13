<template>
  <div id="bg">
    <div id="login-model">
      <div id="banner">
        <div><h2>welcome back :)</h2></div>
      </div>
      <div id="login">
        <div id="welcome">
          <div id="text-content">
            <h2>你好！</h2>
            <h2>欢迎使用！</h2>
          </div>
        </div>
        <div id="login-form">
          <basic-form></basic-form>

        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import BasicForm from "./form/BasicForm.vue";

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

  #login-model {
    width: var(--model-with);
    height: calc(var(--model-with) / 2);
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 15px;
    overflow: hidden;
    display: flex;

    #banner {
      width: calc(var(--model-with) / 2);
      flex: 1;
      color: #FFF;
      display: flex;
      justify-content: center;
      align-items: center;
    }

    #login {
      box-sizing: border-box;
      padding: var(--login-model-padding);
      width: calc(var(--model-with) / 2);
      background-color: #FFF;
      height: 100%;
      flex-direction: column;

      #welcome {
        flex-direction: column;

        #text-content {
          flex-direction: column;
          text-align: left;
          align-items: center;

          * {
            display: block;
            flex: none;
          }
        }
      }

      #login-form {
        margin-top: 30px;
      }
    }


  }
}


</style>
