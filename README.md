# TiSodium [![Appcelerator Titanium](http://www-static.appcelerator.com/badges/titanium-git-badge-sq.png)](http://appcelerator.com/titanium/)

Titanium Mobile binding to [sodium](https://github.com/jedisct1/libsodium/) crypto library based on [sodium-jni](https://github.com/JackWink/sodium-jni).
It implements XSalsa20 encryption.

## Example usage

```coffeescript
sodium = require 'org.farwayer.ti.sodium'


toBuffer: (blob) ->
  buffer = Ti.createBuffer(length: blob.length)
  stream = Ti.Stream.createStream(source: blob, mode: Ti.Stream.MODE_READ)
  stream.read(buffer)
  return buffer


# generate new cipher key
# you must save it in safe place
key = sodium.newKey()

# create new cipher
cipher = sodium.createCipher(key: key)


encrypt: (image, path, success) ->
  # convert Ti.Blob -> Ti.Buffer
  data = toBuffer(image)

  cipher.encrypt
    data: data
    success: (e) ->
      # unique random encryption 'nonce' for current data
      # you must save it anywhere for latter decryption
      # we will save it in the same file
      # but nonce can be saved separately from encrypted data
      # for more security
      nonce = e.nonce

      # ciphertext is Ti.Buffer
      encryptedData = e.ciphertext

      file = Ti.Filesystem.getFile(path)
      stream = file.open(Ti.Filesystem.MODE_WRITE)
      stream.write(nonce)
      stream.write(encryptedData)
      stream.close()

      success()


decrypt: (path, success) ->
  file = Ti.Filesystem.getFile(path)
  stream = file.open(Ti.Filesystem.MODE_READ)

  nonceSize = sodium.NONCE_SIZE
  nonce = Ti.createBuffer(length: nonceSize)
  stream.read(nonce)

  dataSize = file.size - nonceSize
  ciphertext = Ti.createBuffer(length: dataSize)
  stream.read(ciphertext)

  stream.close()

  @cipher.decrypt
    ciphertext: ciphertext
    nonce: nonce
    success: (e) ->
      # e.data is Ti.Buffer
      # convert it to Ti.Blob
      image = e.data.toBlob()
      success(image)


path = "#{Ti.Filesystem.externalStorageDirectory}/encrypted.raw"

Ti.Media.showCamera
  success: (e) ->
    image = e.media

    encrypt image, path, ->
      # encrypted and saved to path, try to decrypt

      decrypt path, (decryptedImage) ->
        # decrypted

```
