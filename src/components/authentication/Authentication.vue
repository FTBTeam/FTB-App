<template>
  <div class='authentication' @click.self='$emit("close")'>
    <div class='body-contents text-center'>
      <h3 class='text-2xl mb-4'><b>Minecraft Login</b></h3>
      <p class='mb-8'>
        Now that Minecraft uses Microsoft to login, we now require to login
        to your Microsoft account or your Mojang account.
      </p>
      
      <h4 class='text-center font-bold mb-8'>Sign in with</h4>
      
      <button class='button'>
        <img src='@/assets/images/branding/microsoft.svg' alt='Microsoft Login' />
      </button>
      
      <div class='or'>
        <span>or</span>
      </div>
      
      <button class='button'>
        <img src='@/assets/images/branding/mojang.svg' alt='Mojang Login' />
      </button>
    </div>
  </div>
</template>

<script lang='ts'>
import Component from 'vue-class-component';
import Vue from 'vue';

@Component
export default class Authentication extends Vue {
  
}
</script>

<style lang='scss' scoped>
.authentication {
  width: 100%;
  height: 100%;
  
  display: flex;
  justify-content: center;
  align-items: center;
  
  position: fixed;
  top: 0;
  left: 0;
  background-color: rgba(#1C1C1C, .82);
  backdrop-filter: blur(2px);
  
  .body-contents {
    background-color: #313131;
    border-radius: 10px;
    box-shadow: 0 6.6px 22.66px 0 #00000040;
    padding: 3rem;
    width: 550px;
    position: relative;
    z-index: 1;
    
    .button {
      height: 100px;
      background-color: #454545;
      width: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 5px;
      box-shadow: 0 0 0 0 #00000040;
      transition: background-color .2s ease-in-out, transform .2s ease-in-out, box-shadow .2s ease-in-out;
      
      &:focus {
        outline: none;
      }
      
      &:hover {
        background-color: #575757;
        transform: translateX(.3rem) translateY(-.3rem);
        box-shadow: -.3rem .3rem 5px 0 #00000040;
      }
    }
    
    .or {
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 1.5rem 0;
      position: relative;
      
      &::after {
        background-color: rgba(white, .2);
        content: '';
        position: absolute;
        top: 50%;
        left: 0;
        transform: translatey(-50%);
        height: 2px;
        border-radius: 4px;
        width: 100%;
        z-index: -1;
      }
      
      span {
        background-color: #313131;
        padding: 0 3rem;
        text-transform: uppercase;
        font-weight: bold;
        color: rgba(white, .25);
      }
    }
  }
}
</style>